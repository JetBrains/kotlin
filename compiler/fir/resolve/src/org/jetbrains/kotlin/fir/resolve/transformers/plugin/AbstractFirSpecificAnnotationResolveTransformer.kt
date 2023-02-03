/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.DesignationState
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS_WITH_ARGUMENTS
import org.jetbrains.kotlin.fir.resolve.transformers.withClassDeclarationCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Deprecated
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DeprecatedSinceKotlin
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRecord
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.WasExperimental
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * @see org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox.Companion
 */
object CompilerRequiredAnnotationsHelper {
    internal val REQUIRED_ANNOTATIONS_WITH_ARGUMENTS: Set<ClassId> = setOf(
        Deprecated,
    )

    val REQUIRED_ANNOTATIONS: Set<ClassId> = REQUIRED_ANNOTATIONS_WITH_ARGUMENTS + setOf(
        DeprecatedSinceKotlin,
        WasExperimental,
        JvmRecord,
    )
}

internal abstract class AbstractFirSpecificAnnotationResolveTransformer(
    protected val session: FirSession,
    protected val scopeSession: ScopeSession,
    protected val computationSession: CompilerRequiredAnnotationsComputationSession,
    containingDeclarations: List<FirDeclaration> = emptyList()
) : FirDefaultTransformer<Nothing?>() {
    companion object {
        private val REQUIRED_ANNOTATION_NAMES: Set<Name> = REQUIRED_ANNOTATIONS.mapTo(mutableSetOf()) { it.shortClassName }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    private val annotationsFromPlugins: Set<AnnotationFqn> = session.registeredPluginAnnotations.annotations
    private val metaAnnotationsFromPlugins: Set<AnnotationFqn> = session.registeredPluginAnnotations.metaAnnotations

    protected open val shouldRecordIntoPredicateBasedProvider: Boolean
        get() = true

    @PrivateForInline
    val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        session,
        errorTypeAsResolved = false
    )

    @PrivateForInline
    val argumentsTransformer = FirAnnotationArgumentsResolveTransformer(
        session,
        scopeSession,
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
    )

    private var owners: PersistentList<FirDeclaration> = persistentListOf()
    private val classDeclarationsStack = ArrayDeque<FirClass>().apply {
        for (declaration in containingDeclarations) {
            if (declaration is FirClass) {
                add(declaration)
            }
        }
    }

    @OptIn(PrivateForInline::class)
    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): FirStatement {
        val annotationTypeRef = annotationCall.annotationTypeRef
        if (annotationTypeRef !is FirUserTypeRef) return annotationCall
        val name = annotationTypeRef.qualifier.last().name

        if (!shouldRunAnnotationResolve(name)) return annotationCall

        val transformedAnnotationType = typeResolverTransformer.transformUserTypeRef(
            annotationTypeRef.createDeepCopy(),
            ScopeClassDeclaration(scopes.asReversed(), classDeclarationsStack)
        ) as? FirResolvedTypeRef ?: return annotationCall

        resolveAnnotationsOnAnnotationIfNeeded(transformedAnnotationType)

        if (!transformedAnnotationType.requiredToSave()) return annotationCall

        annotationCall.replaceAnnotationTypeRef(transformedAnnotationType)
        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.CompilerRequiredAnnotations)
        // TODO: what if we have type alias here?
        if (transformedAnnotationType.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId in REQUIRED_ANNOTATIONS_WITH_ARGUMENTS) {
            argumentsTransformer.transformAnnotation(annotationCall, ResolutionMode.ContextDependent)
        }

        return annotationCall
    }

    private fun resolveAnnotationsOnAnnotationIfNeeded(annotationTypeRef: FirResolvedTypeRef) {
        val symbol = annotationTypeRef.coneType.toRegularClassSymbol(session) ?: return
        val regularClass = symbol.fir
        if (computationSession.annotationsAreResolved(regularClass)) return
        val designation = DesignationState.create(symbol, emptyMap(), includeFile = true) ?: return
        val transformer = FirDesignatedCompilerRequiredAnnotationsResolveTransformer(
            designation.firstDeclaration.moduleData.session,
            scopeSession,
            computationSession,
            designation
        )
        designation.firstDeclaration.transformSingle(transformer, null)
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Nothing?): FirStatement {
        error("Should not be there")
    }

    private fun shouldRunAnnotationResolve(name: Name): Boolean {
        if (metaAnnotationsFromPlugins.isNotEmpty()) return true
        return name in REQUIRED_ANNOTATION_NAMES || annotationsFromPlugins.any { it.shortName() == name }
    }

    private fun FirResolvedTypeRef.requiredToSave(): Boolean {
        val classId = type.classId ?: return false
        return when {
            classId in REQUIRED_ANNOTATIONS -> true
            classId.asSingleFqName() in annotationsFromPlugins -> true
            metaAnnotationsFromPlugins.isEmpty() -> false
            else -> type.markedWithMetaAnnotation(session, metaAnnotationsFromPlugins)
        }
    }

    private fun ConeKotlinType.markedWithMetaAnnotation(session: FirSession, metaAnnotations: Set<AnnotationFqn>): Boolean {
        return toRegularClassSymbol(session).markedWithMetaAnnotationImpl(session, metaAnnotations, includeItself = true, mutableSetOf())
    }


    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
            if (!shouldTransformDeclaration(regularClass)) return regularClass
            computationSession.recordThatAnnotationsAreResolved(regularClass)
            return transformDeclaration(regularClass, data).also {
                val state = beforeTransformingChildren(regularClass)
                regularClass.transformDeclarations(this, data)
                regularClass.transformSuperTypeRefs(this, data)
                afterTransformingChildren(state)
                calculateDeprecations(regularClass)
            } as FirStatement
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirTypeAlias {
        if (!shouldTransformDeclaration(typeAlias)) return typeAlias
        computationSession.recordThatAnnotationsAreResolved(typeAlias)
        return transformDeclaration(typeAlias, data).also {
            calculateDeprecations(typeAlias)
        } as FirTypeAlias
    }

    @OptIn(FirExtensionApiInternals::class)
    override fun transformDeclaration(declaration: FirDeclaration, data: Nothing?): FirDeclaration {
        return (transformAnnotationContainer(declaration, data) as FirDeclaration).also {
            if (shouldRecordIntoPredicateBasedProvider) {
                predicateBasedProvider.registerAnnotatedDeclaration(declaration, owners)
            }
        }
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        if (!shouldTransformDeclaration(file)) return file
        return withFile(file) {
            withFileScopes(file) {
                scopes = createImportingScopes(file, session, scopeSession, useCaching = false)
                val state = beforeTransformingChildren(file)
                try {
                    file.transformDeclarations(this, data)
                } finally {
                    afterTransformingChildren(state)
                }
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withFile(file: FirFile, f: () -> T): T {
        typeResolverTransformer.withFile(file) {
            argumentsTransformer.context.withFile(file, argumentsTransformer.components) {
                return f()
            }
        }
    }

    private fun calculateDeprecations(classLikeDeclaration: FirClassLikeDeclaration) {
        if (classLikeDeclaration.deprecationsProvider == UnresolvedDeprecationProvider) {
            classLikeDeclaration.replaceDeprecationsProvider(
                classLikeDeclaration.getDeprecationsProvider(session)
            )
        }
    }

    protected lateinit var scopes: List<FirScope>

    inline fun <T> withFileScopes(file: FirFile, f: () -> T): T {
        scopes = createImportingScopes(file, session, scopeSession, useCaching = false)
        return f()
    }

    protected abstract fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean

    override fun transformProperty(property: FirProperty, data: Nothing?): FirProperty {
        if (!shouldTransformDeclaration(property)) return property
        computationSession.recordThatAnnotationsAreResolved(property)
        return transformDeclaration(property, data) as FirProperty
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: Nothing?
    ): FirSimpleFunction {
        if (!shouldTransformDeclaration(simpleFunction)) return simpleFunction
        computationSession.recordThatAnnotationsAreResolved(simpleFunction)
        return transformDeclaration(simpleFunction, data).also {
            val state = beforeTransformingChildren(simpleFunction)
            simpleFunction.transformValueParameters(this, data)
            afterTransformingChildren(state)
        } as FirSimpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): FirConstructor {
        if (!shouldTransformDeclaration(constructor)) return constructor
        computationSession.recordThatAnnotationsAreResolved(constructor)
        return transformDeclaration(constructor, data).also {
            val state = beforeTransformingChildren(constructor)
            constructor.transformValueParameters(this, data)
            afterTransformingChildren(state)
        } as FirConstructor
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: Nothing?
    ): FirStatement {
        if (!shouldTransformDeclaration(valueParameter)) return valueParameter
        computationSession.recordThatAnnotationsAreResolved(valueParameter)
        return transformDeclaration(valueParameter, data) as FirStatement
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): FirTypeRef {
        return transformAnnotationContainer(typeRef, data) as FirTypeRef
    }

    override fun transformAnnotationContainer(
        annotationContainer: FirAnnotationContainer,
        data: Nothing?
    ): FirAnnotationContainer {
        return annotationContainer.transformAnnotations(this, data)
    }

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    /**
     * Gets called before transforming [parentDeclaration]'s nested declarations (like in a class of a file).
     *
     * @param parentDeclaration A declaration whose nested declarations are about to be transformed.
     * @return Some state of the transformer; when the nested declarations are transformed, this state will be
     * passed to the [afterTransformingChildren].
     */
    private fun beforeTransformingChildren(parentDeclaration: FirDeclaration): PersistentList<FirDeclaration> {
        val current = owners
        owners = owners.add(parentDeclaration)
        return current
    }


    /**
     * Gets called after performing transformation of some declaration's nested declarations; can be used to restore the internal
     * state of the transformer.
     *
     * @param state A state produced by the [beforeTransformingChildren] call before the transformation.
     */
    private fun afterTransformingChildren(state: PersistentList<FirDeclaration>?) {
        requireNotNull(state)
        owners = state
    }

    private fun FirUserTypeRef.createDeepCopy(): FirUserTypeRef {
        val original = this
        return buildUserTypeRef {
            source = original.source
            isMarkedNullable = original.isMarkedNullable
            annotations.addAll(original.annotations)
            original.qualifier.mapTo(qualifier) { it.createDeepCopy() }
        }
    }

    private fun FirQualifierPart.createDeepCopy(): FirQualifierPart {
        val newArgumentList = FirTypeArgumentListImpl(typeArgumentList.source).apply {
            typeArgumentList.typeArguments.mapTo(typeArguments) { it.createDeepCopy() }
        }
        return FirQualifierPartImpl(
            source,
            name,
            newArgumentList
        )
    }

    private fun FirTypeProjection.createDeepCopy(): FirTypeProjection {
        return when (val original = this) {
            is FirTypeProjectionWithVariance -> buildTypeProjectionWithVariance {
                source = original.source
                typeRef = when (val originalTypeRef = original.typeRef) {
                    is FirUserTypeRef -> originalTypeRef.createDeepCopy()
                    else -> originalTypeRef
                }
                variance = original.variance
            }
            is FirStarProjection -> buildStarProjection { source = original.source }
            is FirPlaceholderProjection -> buildPlaceholderProjection { source = original.source }
            else -> shouldNotBeCalled()
        }
    }
}
