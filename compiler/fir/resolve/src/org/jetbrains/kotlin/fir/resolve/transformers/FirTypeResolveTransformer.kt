/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isFromVararg
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
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

class FirTypeResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirTypeResolveTransformer(session, scopeSession)
}

fun <F : FirClassLikeDeclaration> F.runTypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
): F {
    val transformer = FirTypeResolveTransformer(session, scopeSession, currentScopeList)

    return this.transform<F, Nothing?>(transformer, null)
}

open class FirTypeResolveTransformer(
    final override val session: FirSession,
    private val scopeSession: ScopeSession,
    initialScopes: List<FirScope> = emptyList()
) : FirAbstractTreeTransformer<Any?>(FirResolvePhase.TYPES) {
    private val classDeclarationsStack = ArrayDeque<FirRegularClass>()
    private val scopes = mutableListOf<FirScope>()
    private val towerScope = scopes.asReversed()

    init {
        scopes.addAll(initialScopes.asReversed())
    }

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session)
    private var currentFile: FirFile? = null

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        currentFile = file
        return withScopeCleanup {
            scopes.addAll(createImportingScopes(file, session, scopeSession))
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
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

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        return resolveClassContent(anonymousObject, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: Any?): FirConstructor {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            transformDeclaration(constructor, data)
        } as FirConstructor
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirTypeAlias {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            transformDeclaration(typeAlias, data)
        } as FirTypeAlias
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Any?): FirEnumEntry {
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        return enumEntry
    }

    override fun transformProperty(property: FirProperty, data: Any?): FirProperty {
        return withScopeCleanup {
            property.addTypeParametersScope()
            property.transformTypeParameters(this, data)
                .transformReturnTypeRef(this, data)
                .transformReceiverTypeRef(this, data)
                .transformGetter(this, data)
                .transformSetter(this, data)
                .transformBackingField(this, data)
                .transformAnnotations(this, data)
            if (property.isFromVararg == true) {
                property.transformTypeToArrayType()
                property.getter?.transformReturnTypeRef(StoreType, property.returnTypeRef)
                property.setter?.valueParameters?.map { it.transformReturnTypeRef(StoreType, property.returnTypeRef) }
            }

            unboundCyclesInTypeParametersSupertypes(property)

            property
        }
    }

    override fun transformField(field: FirField, data: Any?): FirField {
        return withScopeCleanup {
            field.transformReturnTypeRef(this, data).transformAnnotations(this, data)
            field
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): FirSimpleFunction {
        return withScopeCleanup {
            simpleFunction.addTypeParametersScope()
            transformDeclaration(simpleFunction, data).also {
                unboundCyclesInTypeParametersSupertypes(it as FirTypeParametersOwner)
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
            typeRef.transform(
                typeResolverTransformer,
                ScopeClassDeclaration(towerScope, classDeclarationsStack)
            )
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): FirStatement {
        valueParameter.transformReturnTypeRef(this, data)
        valueParameter.transformAnnotations(this, data)
        valueParameter.transformVarargTypeToArrayType()
        return valueParameter
    }

    override fun transformBlock(block: FirBlock, data: Any?): FirStatement {
        return block
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Any?): FirStatement {
        annotation.transformAnnotationTypeRef(this, data)
        return annotation
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): FirStatement {
        return transformAnnotation(annotationCall, data)
    }

    private inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        val result = l()
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.removeAt(scopes.lastIndex)
        }
        return result
    }

    private fun resolveClassContent(
        firClass: FirClass,
        data: Any?
    ): FirStatement {

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

        return withScopeCleanup {
            // ? Is it Ok to use original file session here ?
            val superTypes = lookupSuperTypes(
                firClass,
                lookupInterfaces = false,
                deep = true,
                substituteTypes = true,
                useSiteSession = session
            ).asReversed()
            for (superType in superTypes) {
                superType.lookupTag.getNestedClassifierScope(session, scopeSession)?.let { nestedClassifierScope ->
                    val scope = nestedClassifierScope.wrapNestedClassifierScopeWithSubstitutionForSuperType(superType, session)
                    scopes.add(scope)
                }
            }
            session.nestedClassifierScope(firClass)?.let(scopes::add)
            if (firClass is FirRegularClass) {
                val companionObject = firClass.companionObjectSymbol?.fir
                if (companionObject != null) {
                    session.nestedClassifierScope(companionObject)?.let(scopes::add)
                }
                firClass.addTypeParametersScope()
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
            scopes.add(FirMemberTypeParameterScope(this))
        }
    }
}
