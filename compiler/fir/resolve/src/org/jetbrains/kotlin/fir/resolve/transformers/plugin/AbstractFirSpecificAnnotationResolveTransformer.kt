/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
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
        // This transformer is only used for COMPILER_REQUIRED_ANNOTATIONS, which is <=SUPER_TYPES,
        // so we can't yet expand typealiases.
        expandTypeAliases = false,
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

        override fun transformBooleanOperatorExpression(
            booleanOperatorExpression: FirBooleanOperatorExpression,
            data: ResolutionMode,
        ): FirStatement {
            return booleanOperatorExpression
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

        override fun transformIndexedAccessAugmentedAssignment(
            indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment,
            data: ResolutionMode,
        ): FirStatement {
            return indexedAccessAugmentedAssignment
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
        ): FirQualifiedAccessExpression {
            qualifiedAccessExpression.resolveFromImportScope()
            return qualifiedAccessExpression
        }

        private fun FirQualifiedAccessExpression.resolveFromImportScope() {
            val calleeReference = calleeReference as? FirSimpleNamedReference ?: return
            val calleeName = calleeReference.name
            val receiver = explicitReceiver as? FirQualifiedAccessExpression

            if (receiver != null) {
                // Simple case X.Y or fully qualified case a.b.X.Y
                // Resolve receiver from import scope.

                val receiverCalleeReference = receiver.calleeReference as? FirSimpleNamedReference ?: return
                val receiverName = receiverCalleeReference.name.takeIf { !it.isSpecial } ?: return

                val symbol = scopes.firstNotNullOfOrNull {
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
                    coneTypeOrNull = session.builtinTypes.unitType.coneType
                    this.symbol = symbol
                    isFullyQualified = segments.isNotEmpty()
                    resolvedToCompanionObject = false
                }

                // Resolve enum entry by name from the declarations of the receiver.
                val calleeSymbol = symbol.fir.declarations.firstOrNull {
                    it is FirEnumEntry && it.name == calleeName
                }?.symbol as? FirEnumEntrySymbol ?: return

                updateCallee(calleeReference, calleeSymbol)

                replaceExplicitReceiver(resolvedReceiver)
                replaceDispatchReceiver(resolvedReceiver)
            } else {
                // Case where enum entry is explicitly imported.
                val calleeSymbol = scopes.firstNotNullOfOrNull { scope ->
                    if (scope is FirAbstractImportingScope) {
                        @OptIn(FirImplementationDetail::class)
                        scope.findEnumEntryWithoutResolution(calleeName)
                    } else {
                        scope.getProperties(calleeName).firstOrNull()
                    }
                } as? FirEnumEntrySymbol ?: return

                updateCallee(calleeReference, calleeSymbol)
            }
        }

        private fun FirQualifiedAccessExpression.updateCallee(
            calleeReference: FirSimpleNamedReference,
            calleeSymbol: FirEnumEntrySymbol
        ) {
            session.lookupTracker?.recordNameLookup(
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
        get() = session.registeredPluginAnnotations.hasRegisteredAnnotations

    @PrivateForInline
    val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(
        session,
        errorTypeAsResolved = false,
        resolveDeprecations = false,
        // This transformer is only used for COMPILER_REQUIRED_ANNOTATIONS, which is <=SUPER_TYPES,
        // so we can't yet expand typealiases.
        expandTypeAliases = false,
    )

    @PrivateForInline
    val argumentsTransformer: FirEnumAnnotationArgumentsTransformerDispatcher = FirEnumAnnotationArgumentsTransformerDispatcher()

    @PrivateForInline
    var owners: PersistentList<FirDeclaration> = persistentListOf()

    @PrivateForInline
    val classDeclarationsStack: ArrayDeque<FirClass> = ArrayDeque<FirClass>().apply {
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
            data = TypeResolutionConfiguration(scopes.asReversed(), classDeclarationsStack, currentFile),
        ) as? FirResolvedTypeRef ?: return

        resolveAnnotationsOnAnnotationIfNeeded(transformedAnnotationType)

        if (!transformedAnnotationType.requiredToSave()) return

        annotationCall.replaceAnnotationTypeRef(transformedAnnotationType)
        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.CompilerRequiredAnnotations)
        computationSession.annotationResolved(annotationCall)

        val requiredAnnotationsWithArguments = session.annotationPlatformSupport.requiredAnnotationsWithArguments
        if (transformedAnnotationType.coneType.classLikeLookupTagIfAny?.classId in requiredAnnotationsWithArguments) {
            argumentsTransformer.transformAnnotation(annotationCall, ResolutionMode.ContextDependent)
        }
    }

    private fun resolveAnnotationsOnAnnotationIfNeeded(annotationTypeRef: FirResolvedTypeRef) {
        val symbol = annotationTypeRef.coneType.classLikeLookupTagIfAny?.toRegularClassSymbol(session) ?: return
        computationSession.resolveAnnotationsOnAnnotationIfNeeded(symbol, scopeSession)
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: Nothing?): FirStatement {
        error("Should not be there")
    }

    private fun shouldRunAnnotationResolve(typeRef: FirUserTypeRef): Boolean {
        val name = typeRef.shortName
        if (metaAnnotationsFromPlugins.isNotEmpty()) return true
        return name in session.annotationPlatformSupport.requiredAnnotationsShortClassNames || annotationsFromPlugins.any { it.shortName() == name }
    }

    private fun FirResolvedTypeRef.requiredToSave(): Boolean {
        val classId = coneType.classId ?: return false
        return when {
            classId in session.annotationPlatformSupport.requiredAnnotations -> true
            classId.asSingleFqName() in annotationsFromPlugins -> true
            metaAnnotationsFromPlugins.isEmpty() -> false
            else -> coneType.markedWithMetaAnnotation(session, metaAnnotationsFromPlugins)
        }
    }

    private fun ConeKotlinType.markedWithMetaAnnotation(session: FirSession, metaAnnotations: Set<AnnotationFqn>): Boolean {
        return markedWithMetaAnnotationImpl(session, metaAnnotations, includeItself = true, mutableSetOf()) {
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
        )

        return regularClass
    }

    inline fun resolveRegularClass(
        regularClass: FirRegularClass,
        transformChildren: () -> Unit,
    ) {
        withRegularClass(regularClass) {
            if (!shouldTransformDeclaration(regularClass)) return
            if (!computationSession.annotationResolutionWasAlreadyStarted(regularClass)) {
                computationSession.recordThatAnnotationResolutionStarted(regularClass)
                transformDeclaration(regularClass, null)
                computationSession.recordThatAnnotationsAreResolved(regularClass)
            }

            transformChildren(regularClass) {
                regularClass.transformContextParameters(this, null)
                transformChildren()
            }

            calculateDeprecations(regularClass)
        }
    }

    inline fun resolveScript(
        script: FirScript,
        transformChildren: () -> Unit,
    ) {
        if (!shouldTransformDeclaration(script)) return

        computationSession.recordThatAnnotationsAreResolved(script)
        transformDeclaration(script, null).also {
            transformChildren(script) {
                transformChildren()
            }
        }
    }

    override fun transformScript(
        script: FirScript,
        data: Nothing?,
    ): FirScript {
        resolveScript(script) {
            script.transformDeclarations(this, data)
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

    override fun transformDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: Nothing?): FirDanglingModifierList {
        if (!shouldTransformDeclaration(danglingModifierList)) return danglingModifierList
        computationSession.recordThatAnnotationsAreResolved(danglingModifierList)
        return transformDeclaration(danglingModifierList, data).also {
            transformChildren(danglingModifierList) {
                danglingModifierList.transformContextParameters(this, data)
            }
        } as FirDanglingModifierList
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
        resolveFile(file) {
            file.transformDeclarations(this, data)
        }

        return file
    }

    inline fun resolveFile(
        file: FirFile,
        crossinline transformChildren: () -> Unit,
    ) {
        if (!shouldTransformDeclaration(file)) return
        withFileAndFileScopes(file) {
            transformChildren()
        }
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

    @PrivateForInline
    @JvmField
    var currentFile: FirFile? = null

    inline fun <T> withFile(file: FirFile, f: () -> T): T {
        val oldValue = currentFile
        currentFile = file
        return try {
            argumentsTransformer.context.withFile(file, argumentsTransformer.components) {
                withFileAnalysisExceptionWrapping(file, f)
            }
        } finally {
            currentFile = oldValue
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
    ): T where T : FirCallableDeclaration, T : FirStatement {
        if (!shouldTransformDeclaration(callableDeclaration)) return callableDeclaration
        computationSession.recordThatAnnotationsAreResolved(callableDeclaration)

        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(callableDeclaration, data).also {
            transformChildren(callableDeclaration) {
                callableDeclaration.transformContextParameters(this, data)
            }

            calculateDeprecations(callableDeclaration)
        } as T
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
        return transformFunctionDeclarationForDeprecations(propertyAccessor, data)
    }

    private fun <T> transformFunctionDeclarationForDeprecations(
        function: T,
        data: Nothing?,
    ): T where T : FirFunction, T : FirStatement {
        if (!shouldTransformDeclaration(function)) return function
        computationSession.recordThatAnnotationsAreResolved(function)

        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(function, data).also {
            transformChildren(function) {
                function.transformContextParameters(this, data)
                function.transformValueParameters(this, data)
            }

            calculateDeprecations(function)
        } as T
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): FirStatement {
        if (!shouldTransformDeclaration(property)) return property
        computationSession.recordThatAnnotationsAreResolved(property)
        return transformDeclaration(property, data).also {
            property.moveJavaDeprecatedAnnotationToBackingField()

            transformChildren(property) {
                property.transformContextParameters(this, data)
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
    ): FirSimpleFunction = transformFunctionDeclarationForDeprecations(simpleFunction, data)

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): FirConstructor = transformFunctionDeclarationForDeprecations(constructor, data)

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: Nothing?): FirConstructor =
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

    override fun transformErrorProperty(errorProperty: FirErrorProperty, data: Nothing?): FirStatement {
        return transformProperty(errorProperty, data)
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

    private fun createDeepCopyOfTypeRef(original: FirUserTypeRef): FirUserTypeRef = buildUserTypeRef {
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
        }
    }
}
