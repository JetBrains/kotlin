/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.DesignationState
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS_WITH_ARGUMENTS
import org.jetbrains.kotlin.fir.resolve.transformers.withClassDeclarationCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Deprecated
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.DeprecatedSinceKotlin
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JvmRecord
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.WasExperimental
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.Target
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * @see org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox.Companion
 */
object CompilerRequiredAnnotationsHelper {
    internal val REQUIRED_ANNOTATIONS_WITH_ARGUMENTS: Set<ClassId> = setOf(
        Deprecated,
        Target,
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

    inner class FirEnumAnnotationArgumentsTransformerDispatcher : FirAbstractBodyResolveTransformerDispatcher(
        session,
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
        scopeSession = scopeSession,
        implicitTypeOnly = false,
    ) {
        override val expressionsTransformer: FirExpressionsResolveTransformer = FirEnumAnnotationArgumentsTransformer(this)
        override val declarationsTransformer: FirDeclarationsResolveTransformer get() = throw NotImplementedError()
    }

    /**
     * Special transformer that resolves qualified expressions exclusively to enums from import scope. This doesn't
     * trigger body resolve.
     */
    private inner class FirEnumAnnotationArgumentsTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
        AbstractFirExpressionsResolveTransformerForAnnotations(transformer) {

        override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement {
            // transform arrayOf arguments to handle `@Foo(bar = arrayOf(X))`
            functionCall.transformChildren(transformer, data)
            return super.transformFunctionCall(functionCall, data)
        }

        override fun resolveQualifiedAccessAndSelectCandidate(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            isUsedAsReceiver: Boolean,
            callSite: FirElement
        ): FirStatement {
            qualifiedAccessExpression.resolveFromImportScope()
            return qualifiedAccessExpression
        }

        private fun FirQualifiedAccessExpression.resolveFromImportScope() {
            val calleeReference = calleeReference as? FirSimpleNamedReference ?: return
            val receiver = explicitReceiver as? FirQualifiedAccessExpression

            if (receiver != null) {
                // Simple case X.Y or fully qualified case a.b.X.Y
                // Resolve receiver from import scope.

                val receiverCalleeReference = receiver.calleeReference as? FirSimpleNamedReference ?: return
                val receiverName = receiverCalleeReference.name.takeIf { !it.isSpecial } ?: return

                val symbol = scopes.filterIsInstance<FirAbstractImportingScope>().firstNotNullOfOrNull {
                    it.getSingleClassifier(receiverName) as? FirClassSymbol<*>
                } ?: return

                // If fully qualified, check that given package name matches the resolved one.
                val segments = generateSequence(receiver.explicitReceiver) { (it as? FirQualifiedAccessExpression)?.explicitReceiver }
                    .mapNotNull { (it.calleeReference as? FirSimpleNamedReference)?.name?.identifier }
                    .toList()

                if (segments.isNotEmpty() && FqName.fromSegments(segments.asReversed()) != symbol.classId.packageFqName) {
                    return
                }

                val resolvedReceiver = buildResolvedQualifier {
                    source = receiver.source
                    packageFqName = symbol.classId.packageFqName
                    relativeClassFqName = symbol.classId.relativeClassName
                    typeRef = FirImplicitUnitTypeRef(receiver.typeRef.source)
                    this.symbol = symbol
                    isFullyQualified = segments.isNotEmpty()
                }

                // Resolve enum entry by name from the declarations of the receiver.
                val calleeSymbol = symbol.fir.declarations.firstOrNull {
                    it is FirEnumEntry && it.name == calleeReference.name
                }?.symbol as? FirEnumEntrySymbol ?: return

                updateCallee(calleeReference, calleeSymbol)

                replaceExplicitReceiver(resolvedReceiver)
                replaceDispatchReceiver(resolvedReceiver)
            } else {
                // Case where enum entry is explicitly imported.
                val calleeSymbol = scopes.firstNotNullOfOrNull {
                    it.getProperties(calleeReference.name).firstOrNull()
                } as? FirEnumEntrySymbol ?: return

                updateCallee(calleeReference, calleeSymbol)
            }
        }

        private fun FirQualifiedAccessExpression.updateCallee(
            calleeReference: FirSimpleNamedReference,
            calleeSymbol: FirEnumEntrySymbol
        ) {
            session.lookupTracker?.recordLookup(
                calleeReference.name,
                calleeSymbol.dispatchReceiverType?.classId?.asFqNameString() ?: calleeSymbol.callableId.packageName.asString(),
                this.source,
                context.file.source,
            )

            replaceCalleeReference(buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeSymbol
            })

            calleeSymbol.containingClassLookupTag()
                ?.let { ConeClassLikeTypeImpl(it, emptyArray(), false) }
                ?.let { replaceTypeRef(typeRef.resolvedTypeFromPrototype(it)) }
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    private val annotationsFromPlugins: Set<AnnotationFqn> = session.registeredPluginAnnotations.annotations
    private val metaAnnotationsFromPlugins: Set<AnnotationFqn> = session.registeredPluginAnnotations.metaAnnotations

    protected open val shouldRecordIntoPredicateBasedProvider: Boolean
        get() = true

    @PrivateForInline
    val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        session,
        errorTypeAsResolved = false,
        resolveDeprecations = false,
    )

    @PrivateForInline
    val argumentsTransformer = FirEnumAnnotationArgumentsTransformerDispatcher()

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
                return withFileAnalysisExceptionWrapping(file) {
                    f()
                }
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
