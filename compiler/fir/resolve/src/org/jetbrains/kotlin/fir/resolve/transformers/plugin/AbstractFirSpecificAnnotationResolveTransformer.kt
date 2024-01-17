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
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.withClassDeclarationCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@OptIn(PrivateForInline::class)
abstract class AbstractFirSpecificAnnotationResolveTransformer(
    @property:PrivateForInline val session: FirSession,
    @property:PrivateForInline val scopeSession: ScopeSession,
    @property:PrivateForInline val computationSession: CompilerRequiredAnnotationsComputationSession,
    containingDeclarations: List<FirDeclaration> = emptyList()
) : FirDefaultTransformer<Nothing?>() {
    inner class FirEnumAnnotationArgumentsTransformerDispatcher : FirAbstractBodyResolveTransformerDispatcher(
        session,
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
        scopeSession = scopeSession,
        implicitTypeOnly = false,
    ) {
        override val expressionsTransformer: FirExpressionsResolveTransformer = FirEnumAnnotationArgumentsTransformer(this)
        override val declarationsTransformer: FirDeclarationsResolveTransformer? = null
    }

    /**
     * Special transformer that resolves qualified expressions exclusively to enums from import scope. This doesn't
     * trigger body resolve.
     */
    private inner class FirEnumAnnotationArgumentsTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
        FirExpressionsResolveTransformer(transformer) {
        override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
            dataFlowAnalyzer.enterAnnotation()
            annotation.transformChildren(transformer, ResolutionMode.ContextDependent)
            dataFlowAnalyzer.exitAnnotation()
            return annotation
        }

        override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
            return transformAnnotation(annotationCall, data)
        }

        override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
            return transformAnnotation(errorAnnotationCall, data)
        }

        override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
            return expression.transformChildren(transformer, data) as FirStatement
        }

        override fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
            return calleeReference !is FirErrorNamedReference
        }

        override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
            return block
        }

        override fun transformThisReceiverExpression(
            thisReceiverExpression: FirThisReceiverExpression,
            data: ResolutionMode,
        ): FirStatement {
            return thisReceiverExpression
        }

        override fun transformComparisonExpression(
            comparisonExpression: FirComparisonExpression,
            data: ResolutionMode,
        ): FirStatement {
            return comparisonExpression
        }

        override fun transformTypeOperatorCall(
            typeOperatorCall: FirTypeOperatorCall,
            data: ResolutionMode,
        ): FirStatement {
            return typeOperatorCall
        }

        override fun transformCheckNotNullCall(
            checkNotNullCall: FirCheckNotNullCall,
            data: ResolutionMode,
        ): FirStatement {
            return checkNotNullCall
        }

        override fun transformBinaryLogicExpression(
            binaryLogicExpression: FirBinaryLogicExpression,
            data: ResolutionMode,
        ): FirStatement {
            return binaryLogicExpression
        }

        override fun transformVariableAssignment(
            variableAssignment: FirVariableAssignment,
            data: ResolutionMode,
        ): FirStatement {
            return variableAssignment
        }

        override fun transformCallableReferenceAccess(
            callableReferenceAccess: FirCallableReferenceAccess,
            data: ResolutionMode,
        ): FirStatement {
            return callableReferenceAccess
        }

        override fun transformDelegatedConstructorCall(
            delegatedConstructorCall: FirDelegatedConstructorCall,
            data: ResolutionMode,
        ): FirStatement {
            return delegatedConstructorCall
        }

        override fun transformAugmentedArraySetCall(
            augmentedArraySetCall: FirAugmentedArraySetCall,
            data: ResolutionMode,
        ): FirStatement {
            return augmentedArraySetCall
        }

        override fun transformArrayLiteral(arrayLiteral: FirArrayLiteral, data: ResolutionMode): FirStatement {
            arrayLiteral.transformChildren(transformer, data)
            return arrayLiteral
        }

        override fun transformAnonymousObjectExpression(
            anonymousObjectExpression: FirAnonymousObjectExpression,
            data: ResolutionMode,
        ): FirStatement {
            return anonymousObjectExpression
        }

        override fun transformAnonymousFunctionExpression(
            anonymousFunctionExpression: FirAnonymousFunctionExpression,
            data: ResolutionMode,
        ): FirStatement {
            return anonymousFunctionExpression
        }

        override fun shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall: FirGetClassCall): Boolean {
            return false
        }

        override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement {
            // transform arrayOf arguments to handle `@Foo(bar = arrayOf(X))`
            functionCall.transformChildren(transformer, data)
            return functionCall
        }

        override fun resolveQualifiedAccessAndSelectCandidate(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            isUsedAsReceiver: Boolean,
            isUsedAsGetClassReceiver: Boolean,
            callSite: FirElement,
            data: ResolutionMode,
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
                    .mapNotNull { (it.toReference(session) as? FirSimpleNamedReference)?.name?.identifier }
                    .toList()

                if (segments.isNotEmpty() && FqName.fromSegments(segments.asReversed()) != symbol.classId.packageFqName) {
                    return
                }

                val resolvedReceiver = buildResolvedQualifier {
                    source = receiver.source
                    packageFqName = symbol.classId.packageFqName
                    relativeClassFqName = symbol.classId.relativeClassName
                    coneTypeOrNull = session.builtinTypes.unitType.type
                    this.symbol = symbol
                    isFullyQualified = segments.isNotEmpty()
                }

                // Resolve enum entry by name from the declarations of the receiver.
                val calleeSymbol = symbol.fir.declarations.firstOrNull {
                    it is FirEnumEntry && it.name == calleeReference.name
                }?.symbol as? FirEnumEntrySymbol ?: return

                val enhancedCalleeSymbol = session.compilerRequiredAnnotationEnhancementProvider?.enhance(symbol, calleeSymbol, session)
                    ?: calleeSymbol

                updateCallee(calleeReference, enhancedCalleeSymbol)

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
                ?.let { replaceConeTypeOrNull(it) }
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

    @PrivateForInline
    var owners: PersistentList<FirDeclaration> = persistentListOf()

    @PrivateForInline
    val classDeclarationsStack = ArrayDeque<FirClass>().apply {
        for (declaration in containingDeclarations) {
            if (declaration is FirClass) {
                add(declaration)
            }
        }
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): FirStatement {
        val annotationTypeRef = annotationCall.annotationTypeRef
        if (annotationTypeRef !is FirUserTypeRef) return annotationCall
        if (!shouldRunAnnotationResolve(annotationTypeRef)) return annotationCall
        transformAnnotationCall(annotationCall, annotationTypeRef)
        return annotationCall
    }

    fun transformAnnotationCall(annotationCall: FirAnnotationCall, typeRef: FirUserTypeRef) {
        val transformedAnnotationType = typeResolverTransformer.transformUserTypeRef(
            userTypeRef = createDeepCopyOfTypeRef(typeRef),
            data = ScopeClassDeclaration(scopes.asReversed(), classDeclarationsStack),
        ) as? FirResolvedTypeRef ?: return

        resolveAnnotationsOnAnnotationIfNeeded(transformedAnnotationType)

        if (!transformedAnnotationType.requiredToSave()) return

        annotationCall.replaceAnnotationTypeRef(transformedAnnotationType)
        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.CompilerRequiredAnnotations)

        val requiredAnnotationsWithArguments = session.annotationPlatformSupport.requiredAnnotationsWithArguments

        if (transformedAnnotationType.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId in requiredAnnotationsWithArguments) {
            argumentsTransformer.transformAnnotation(annotationCall, ResolutionMode.ContextDependent)
        }
    }

    private fun resolveAnnotationsOnAnnotationIfNeeded(annotationTypeRef: FirResolvedTypeRef) {
        val symbol = annotationTypeRef.coneType.toRegularClassSymbol(session) ?: return
        computationSession.resolveAnnotationsOnAnnotationIfNeeded(symbol, scopeSession)
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Nothing?): FirStatement {
        error("Should not be there")
    }

    fun shouldRunAnnotationResolve(typeRef: FirUserTypeRef): Boolean {
        val name = typeRef.qualifier.last().name
        if (metaAnnotationsFromPlugins.isNotEmpty()) return true
        return name in session.annotationPlatformSupport.requiredAnnotationsShortClassNames || annotationsFromPlugins.any { it.shortName() == name }
    }

    private fun FirResolvedTypeRef.requiredToSave(): Boolean {
        val classId = type.classId ?: return false
        return when {
            classId in session.annotationPlatformSupport.requiredAnnotations -> true
            classId.asSingleFqName() in annotationsFromPlugins -> true
            metaAnnotationsFromPlugins.isEmpty() -> false
            else -> type.markedWithMetaAnnotation(session, metaAnnotationsFromPlugins)
        }
    }

    private fun ConeKotlinType.markedWithMetaAnnotation(session: FirSession, metaAnnotations: Set<AnnotationFqn>): Boolean {
        return toRegularClassSymbol(session).markedWithMetaAnnotationImpl(session, metaAnnotations, includeItself = true, mutableSetOf()) {
            computationSession.resolveAnnotationsOnAnnotationIfNeeded(it, scopeSession)
            it.annotations
        }
    }


    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        resolveRegularClass(
            regularClass,
            transformChildren = {
                regularClass.transformDeclarations(this, data)
            },
            afterChildrenTransform = {
                calculateDeprecations(regularClass)
            }
        )
        return regularClass
    }

    inline fun resolveRegularClass(
        regularClass: FirRegularClass,
        transformChildren: () -> Unit,
        afterChildrenTransform: () -> Unit
    ) {
        withRegularClass(regularClass) {
            if (!shouldTransformDeclaration(regularClass)) return
            computationSession.recordThatAnnotationsAreResolved(regularClass)
            transformDeclaration(regularClass, null).also {
                transformChildren(regularClass) {
                    transformChildren()
                }
                afterChildrenTransform()
            }
        }
    }

    override fun transformScript(
        script: FirScript,
        data: Nothing?,
    ): FirScript {
        if (shouldTransformDeclaration(script)) {
            computationSession.recordThatAnnotationsAreResolved(script)
            transformDeclaration(script, null).also {
                transformChildren(script) {
                    script.transformDeclarations(this, data)
                }
            }
        }
        return script
    }

    inline fun withRegularClass(
        regularClass: FirRegularClass,
        action: () -> Unit
    ) {
        withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
            action()
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
        withFileAndFileScopes(file) {
            file.transformDeclarations(this, data)
        }

        return file
    }

    fun withFileAndFileScopes(file: FirFile, action: () -> Unit) {
        withFile(file) {
            withFileScopes(file) {
                transformChildren(file) {
                    action()
                }
            }
        }
    }

    inline fun <T> withFile(file: FirFile, f: () -> T): T {
        typeResolverTransformer.withFile(file) {
            argumentsTransformer.context.withFile(file, argumentsTransformer.components) {
                return withFileAnalysisExceptionWrapping(file) {
                    f()
                }
            }
        }
    }

    fun calculateDeprecations(classLikeDeclaration: FirClassLikeDeclaration) {
        if (classLikeDeclaration.deprecationsProvider == UnresolvedDeprecationProvider) {
            classLikeDeclaration.replaceDeprecationsProvider(classLikeDeclaration.getDeprecationsProvider(session))
        }
    }

    private fun calculateDeprecations(callableDeclaration: FirCallableDeclaration) {
        if (callableDeclaration.deprecationsProvider == UnresolvedDeprecationProvider) {
            callableDeclaration.replaceDeprecationsProvider(callableDeclaration.getDeprecationsProvider(session))
        }
    }

    private fun <T> transformCallableDeclarationForDeprecations(
        callableDeclaration: T,
        data: Nothing?,
    ): FirStatement where T : FirCallableDeclaration, T : FirStatement {
        if (!shouldTransformDeclaration(callableDeclaration)) return callableDeclaration
        computationSession.recordThatAnnotationsAreResolved(callableDeclaration)
        return transformDeclaration(callableDeclaration, data).also {
            calculateDeprecations(callableDeclaration)
        } as FirStatement
    }

    lateinit var scopes: List<FirScope>

    inline fun <T> withFileScopes(file: FirFile, f: () -> T): T {
        scopes = createImportingScopes(file, session, scopeSession, useCaching = computationSession.useCacheForImportScope)
        return f()
    }

    abstract fun shouldTransformDeclaration(declaration: FirDeclaration): Boolean

    override fun transformBackingField(backingField: FirBackingField, data: Nothing?): FirStatement {
        return transformCallableDeclarationForDeprecations(backingField, data)
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Nothing?): FirStatement {
        return transformCallableDeclarationForDeprecations(propertyAccessor, data)
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): FirStatement {
        if (!shouldTransformDeclaration(property)) return property
        computationSession.recordThatAnnotationsAreResolved(property)
        return transformDeclaration(property, data).also {
            property.moveJavaDeprecatedAnnotationToBackingField()

            transformChildren(property) {
                property.transformSetter(this, data)
                property.transformGetter(this, data)
                property.transformBackingField(this, data)
            }

            calculateDeprecations(property)
        } as FirStatement
    }

    private fun FirProperty.moveJavaDeprecatedAnnotationToBackingField() {
        val newPosition = session.annotationPlatformSupport.extractBackingFieldAnnotationsFromProperty(this, session) ?: return
        this.replaceAnnotations(newPosition.propertyAnnotations)
        backingField?.replaceAnnotations(newPosition.backingFieldAnnotations)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: Nothing?
    ): FirSimpleFunction {
        if (!shouldTransformDeclaration(simpleFunction)) return simpleFunction
        computationSession.recordThatAnnotationsAreResolved(simpleFunction)
        return transformDeclaration(simpleFunction, data).also {
            transformChildren(simpleFunction) {
                simpleFunction.transformValueParameters(this, data)
            }

            calculateDeprecations(simpleFunction)
        } as FirSimpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): FirConstructor {
        if (!shouldTransformDeclaration(constructor)) return constructor
        computationSession.recordThatAnnotationsAreResolved(constructor)
        return transformDeclaration(constructor, data).also {
            transformChildren(constructor) {
                constructor.transformValueParameters(this, data)
            }

            calculateDeprecations(constructor)
        } as FirConstructor
    }

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: Nothing?) =
        transformConstructor(errorPrimaryConstructor, data)

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): FirStatement {
        return transformCallableDeclarationForDeprecations(enumEntry, data)
    }

    override fun transformField(field: FirField, data: Nothing?): FirStatement {
        return transformCallableDeclarationForDeprecations(field, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): FirStatement {
        return transformCallableDeclarationForDeprecations(valueParameter, data)
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
    fun beforeTransformingChildren(parentDeclaration: FirDeclaration): PersistentList<FirDeclaration> {
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
    fun afterTransformingChildren(state: PersistentList<FirDeclaration>?) {
        requireNotNull(state)
        owners = state
    }

    inline fun <R> transformChildren(parentDeclaration: FirDeclaration, action: () -> R): R {
        val state = beforeTransformingChildren(parentDeclaration)
        try {
            return action()
        } finally {
            afterTransformingChildren(state)
        }
    }

    fun createDeepCopyOfTypeRef(original: FirUserTypeRef): FirUserTypeRef = buildUserTypeRef {
        source = original.source
        isMarkedNullable = original.isMarkedNullable
        annotations.addAll(original.annotations)
        original.qualifier.mapTo(qualifier) { it.createDeepCopy() }
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
                    is FirUserTypeRef -> createDeepCopyOfTypeRef(originalTypeRef)
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
