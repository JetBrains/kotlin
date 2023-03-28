/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.copyWithNewSourceKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFromVararg
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguouslyResolvedAnnotationFromPlugin
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCyclicTypeBound
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.whileAnalysing
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirTypeResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.TYPES) {
    override val transformer = FirTypeResolveTransformer(session, scopeSession)
}

fun <F : FirClassLikeDeclaration> F.runTypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
    useSiteFile: FirFile,
    containingDeclarations: List<FirDeclaration>,
): F {
    val transformer = FirTypeResolveTransformer(
        session,
        scopeSession,
        currentScopeList,
        initialCurrentFile = useSiteFile,
        classDeclarationsStack = containingDeclarations.filterIsInstanceTo(ArrayDeque())
    )

    return this.transform(transformer, null)
}

open class FirTypeResolveTransformer(
    final override val session: FirSession,
    private val scopeSession: ScopeSession,
    initialScopes: List<FirScope> = emptyList(),
    initialCurrentFile: FirFile? = null,
    private val classDeclarationsStack: ArrayDeque<FirClass> = ArrayDeque()
) : FirAbstractTreeTransformer<Any?>(FirResolvePhase.TYPES) {
    /**
     * All current scopes sorted from outermost to innermost.
     */
    private var scopes = initialScopes.asReversed().toPersistentList()

    /**
     * Scopes that are accessible statically, i.e. [scopes] minus type parameter scopes.
     */
    private var staticScopes = scopes

    private var currentDeclaration: FirDeclaration? = null

    private inline fun <T> withDeclaration(declaration: FirDeclaration, crossinline action: () -> T): T {
        val oldDeclaration = currentDeclaration
        return try {
            currentDeclaration = declaration
            action()
        } finally {
            currentDeclaration = oldDeclaration
        }
    }

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session)
    private var currentFile: FirFile? = initialCurrentFile

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        currentFile = file
        return withScopeCleanup {
            addScopes(createImportingScopes(file, session, scopeSession))
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        whileAnalysing(session, regularClass) {
            withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
                withScopeCleanup {
                    regularClass.addTypeParametersScope()
                    regularClass.typeParameters.forEach {
                        it.accept(this, data)
                    }
                    unboundCyclesInTypeParametersSupertypes(regularClass)
                }

                return resolveClassContent(regularClass, data)
            }
        }
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, anonymousObject) {
            return resolveClassContent(anonymousObject, data)
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: Any?): FirConstructor = whileAnalysing(session, constructor) {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            val result = transformDeclaration(constructor, data) as FirConstructor

            if (result.isPrimary) {
                for (valueParameter in result.valueParameters) {
                    if (valueParameter.correspondingProperty != null) {
                        valueParameter.removeDuplicateAnnotationsOfPrimaryConstructorElement()
                    }
                }
            }

            calculateDeprecations(result)
            result
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirTypeAlias = whileAnalysing(session, typeAlias) {
        withScopeCleanup {
            typeAlias.addTypeParametersScope()
            transformDeclaration(typeAlias, data)
        } as FirTypeAlias
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Any?): FirEnumEntry = whileAnalysing(session, enumEntry) {
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        calculateDeprecations(enumEntry)
        enumEntry
    }

    override fun transformReceiverParameter(receiverParameter: FirReceiverParameter, data: Any?): FirReceiverParameter {
        return receiverParameter.transformAnnotations(this, data).transformTypeRef(this, data)
    }

    override fun transformProperty(property: FirProperty, data: Any?): FirProperty = whileAnalysing(session, property) {
        withScopeCleanup {
            withDeclaration(property) {
                property.addTypeParametersScope()
                property.transformTypeParameters(this, data)
                    .transformReturnTypeRef(this, data)
                    .transformReceiverParameter(this, data)
                    .transformContextReceivers(this, data)
                    .transformGetter(this, data)
                    .transformSetter(this, data)
                    .transformBackingField(this, data)
                    .transformAnnotations(this, data)

                if (property.isFromVararg == true) {
                    property.transformTypeToArrayType()
                    property.backingField?.transformTypeToArrayType()
                    setAccessorTypesByPropertyType(property)
                }

                when {
                    property.returnTypeRef is FirResolvedTypeRef && property.delegate != null -> {
                        setAccessorTypesByPropertyType(property)
                    }
                    property.returnTypeRef !is FirResolvedTypeRef && property.initializer == null &&
                            property.getter?.returnTypeRef is FirResolvedTypeRef -> {
                        property.replaceReturnTypeRef(
                            property.getter!!.returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType)
                        )
                    }
                }

                unboundCyclesInTypeParametersSupertypes(property)

                if (property.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                    property.removeDuplicateAnnotationsOfPrimaryConstructorElement()
                }

                calculateDeprecations(property)
                property
            }
        }
    }

    private fun setAccessorTypesByPropertyType(property: FirProperty) {
        property.getter?.replaceReturnTypeRef(property.returnTypeRef)
        property.setter?.valueParameters?.map { it.replaceReturnTypeRef(property.returnTypeRef) }
    }

    override fun transformField(field: FirField, data: Any?): FirField = whileAnalysing(session, field) {
        withScopeCleanup {
            field.transformReturnTypeRef(this, data).transformAnnotations(this, data)
            calculateDeprecations(field)
            field
        }
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: Any?,
    ): FirSimpleFunction = whileAnalysing(session, simpleFunction) {
        withScopeCleanup {
            withDeclaration(simpleFunction) {
                simpleFunction.addTypeParametersScope()
                transformDeclaration(simpleFunction, data).also {
                    unboundCyclesInTypeParametersSupertypes(it as FirTypeParametersOwner)
                    calculateDeprecations(simpleFunction)
                }
            }
        } as FirSimpleFunction
    }

    private fun unboundCyclesInTypeParametersSupertypes(typeParametersOwner: FirTypeParameterRefsOwner) {
        for (typeParameter in typeParametersOwner.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            if (hasSupertypePathToParameter(typeParameter, typeParameter, mutableSetOf())) {
                val errorType = buildErrorTypeRef {
                    diagnostic = ConeCyclicTypeBound(typeParameter.symbol, typeParameter.bounds.toImmutableList())
                }
                typeParameter.replaceBounds(
                    listOf(errorType)
                )
            }
        }
    }

    private fun hasSupertypePathToParameter(
        currentTypeParameter: FirTypeParameter,
        typeParameter: FirTypeParameter,
        visited: MutableSet<FirTypeParameter>
    ): Boolean {
        if (visited.isNotEmpty() && currentTypeParameter == typeParameter) return true
        if (!visited.add(currentTypeParameter)) return false

        return currentTypeParameter.bounds.any {
            val nextTypeParameter = it.coneTypeSafe<ConeTypeParameterType>()?.lookupTag?.typeParameterSymbol?.fir ?: return@any false

            hasSupertypePathToParameter(nextTypeParameter, typeParameter, visited)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): FirTypeRef {
        return implicitTypeRef
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): FirResolvedTypeRef {
        return typeResolverTransformer.withFile(currentFile) {
            val scopes = scopes
            // optimized implementation of reversed Iterable on top of PersistentList
            val reversedScopes = object : Iterable<FirScope> {
                override fun iterator() = object : Iterator<FirScope> {
                    private val iter = scopes.listIterator(scopes.size)
                    override fun hasNext() = iter.hasPrevious()
                    override fun next() = iter.previous()
                }
            }
            typeRef.transform(
                typeResolverTransformer,
                ScopeClassDeclaration(reversedScopes, classDeclarationsStack, containerDeclaration = currentDeclaration)
            )
        }
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: Any?,
    ): FirStatement = whileAnalysing(session, valueParameter) {
        withDeclaration(valueParameter) {
            valueParameter.transformReturnTypeRef(this, data)
            valueParameter.transformAnnotations(this, data)
            valueParameter.transformVarargTypeToArrayType()
            calculateDeprecations(valueParameter)
            valueParameter
        }
    }

    override fun transformBlock(block: FirBlock, data: Any?): FirStatement {
        return block
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Any?): FirStatement {
        shouldNotBeCalled()
    }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: Any?
    ): FirStatement = whileAnalysing(session, annotationCall) {
        when (val originalTypeRef = annotationCall.annotationTypeRef) {
            is FirResolvedTypeRef -> {
                when (annotationCall.annotationResolvePhase) {
                    FirAnnotationResolvePhase.Unresolved -> when (originalTypeRef) {
                        is FirErrorTypeRef -> return annotationCall.also { it.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types) }
                        else -> shouldNotBeCalled()
                    }
                    FirAnnotationResolvePhase.CompilerRequiredAnnotations -> {
                        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types)
                        val alternativeResolvedTypeRef =
                            originalTypeRef.delegatedTypeRef?.transformSingle(this, data) ?: return annotationCall
                        val coneTypeFromCompilerRequiredPhase = originalTypeRef.coneType
                        val coneTypeFromTypesPhase = alternativeResolvedTypeRef.coneType
                        if (coneTypeFromTypesPhase != coneTypeFromCompilerRequiredPhase) {
                            val errorTypeRef = buildErrorTypeRef {
                                source = originalTypeRef.source
                                type = coneTypeFromCompilerRequiredPhase
                                delegatedTypeRef = originalTypeRef.delegatedTypeRef
                                diagnostic = ConeAmbiguouslyResolvedAnnotationFromPlugin(
                                    coneTypeFromCompilerRequiredPhase,
                                    coneTypeFromTypesPhase
                                )
                            }
                            annotationCall.replaceAnnotationTypeRef(errorTypeRef)
                        }
                    }
                    FirAnnotationResolvePhase.Types -> {}
                }
            }
            else -> {
                val transformedTypeRef = originalTypeRef.transformSingle(this, data)
                annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types)
                annotationCall.replaceAnnotationTypeRef(transformedTypeRef)
            }
        }
        return annotationCall
    }

    private inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val scopesBefore = scopes
        val staticScopesBefore = staticScopes

        val result = l()

        scopes = scopesBefore
        staticScopes = staticScopesBefore

        return result
    }

    private fun resolveClassContent(
        firClass: FirClass,
        data: Any?
    ): FirStatement {

        return withScopeCleanup {
            // Remove type parameter scopes for classes that are neither inner nor local
            if (!firClass.isInner && !firClass.isLocal) {
                this.scopes = staticScopes
            }

            withScopeCleanup {
                firClass.transformAnnotations(this, null)

                if (firClass is FirRegularClass) {
                    firClass.addTypeParametersScope()
                }

                // ConstructedTypeRef should be resolved only with type parameters, but not with nested classes and classes from supertypes
                for (constructor in firClass.declarations.filterIsInstance<FirConstructor>()) {
                    constructor.delegatedConstructor?.let(this::resolveConstructedTypeRefForDelegatedConstructorCall)
                }
            }

            // ? Is it Ok to use original file session here ?
            val superTypes = lookupSuperTypes(
                firClass,
                lookupInterfaces = false,
                deep = true,
                substituteTypes = true,
                useSiteSession = session
            ).asReversed()

            val scopesToAdd = mutableListOf<FirScope>()

            for (superType in superTypes) {
                superType.lookupTag.getNestedClassifierScope(session, scopeSession)?.let { nestedClassifierScope ->
                    val scope = nestedClassifierScope.wrapNestedClassifierScopeWithSubstitutionForSuperType(superType, session)
                    scopesToAdd.add(scope)
                }
            }
            session.nestedClassifierScope(firClass)?.let(scopesToAdd::add)
            if (firClass is FirRegularClass) {
                val companionObject = firClass.companionObjectSymbol?.fir
                if (companionObject != null) {
                    session.nestedClassifierScope(companionObject)?.let(scopesToAdd::add)
                }

                addScopes(scopesToAdd)
                firClass.addTypeParametersScope()
            } else {
                addScopes(scopesToAdd)
            }

            // Note that annotations are still visited here
            // again, although there's no need in it
            transformElement(firClass, data)
        }
    }

    private fun resolveConstructedTypeRefForDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall
    ) {
        delegatedConstructorCall.replaceConstructedTypeRef(
            delegatedConstructorCall.constructedTypeRef.transform<FirTypeRef, Any?>(this, null)
        )

        delegatedConstructorCall.transformCalleeReference(this, null)
    }

    private fun FirMemberDeclaration.addTypeParametersScope() {
        if (typeParameters.isNotEmpty()) {
            scopes = scopes.add(FirMemberTypeParameterScope(this))
        }
    }

    private fun addScopes(list: List<FirScope>) {
        // small optimization to skip unnecessary allocations
        val scopesAreTheSame = scopes === staticScopes

        scopes = scopes.addAll(list)
        staticScopes = if (scopesAreTheSame) scopes else staticScopes.addAll(list)
    }

    /**
     * In a scenario like
     *
     * ```
     * annotation class Ann
     * class Foo(@Ann val x: String)
     * ```
     *
     * both, the primary ctor value parameter and the property `x` will be annotated with `@Ann`. This is due to the fact, that the
     * annotation needs to be resolved in order to determine its annotation targets. We remove annotations from the wrong target if they
     * don't explicitly specify the use-site target (in which case they shouldn't have been added to the element in the raw FIR).
     *
     * For value parameters, we remove the annotation if the targets don't include [AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER].
     * For properties, we remove the annotation, if the targets include [AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER].
     */
    private fun FirVariable.removeDuplicateAnnotationsOfPrimaryConstructorElement() {
        val isParameter = this is FirValueParameter
        replaceAnnotations(annotations.filter {
            it.useSiteTarget != null ||
                    // equivalent to
                    // CONSTRUCTOR_PARAMETER in targets && isParameter ||
                    // CONSTRUCTOR_PARAMETER !in targets && !isParameter
                    AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER in it.useSiteTargetsFromMetaAnnotation(session) == isParameter
        })
    }

    private fun calculateDeprecations(callableDeclaration: FirCallableDeclaration) {
        if (callableDeclaration.deprecationsProvider is UnresolvedDeprecationProvider) {
            callableDeclaration.replaceDeprecationsProvider(callableDeclaration.getDeprecationsProvider(session))
        }
    }
}
