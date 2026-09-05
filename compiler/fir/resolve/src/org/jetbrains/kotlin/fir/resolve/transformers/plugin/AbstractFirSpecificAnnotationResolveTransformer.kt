/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationArgumentMappingImpl
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedArgumentDuringCompilerRequiredAnnotations
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.withClassDeclarationCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.PrivateForInline

@OptIn(PrivateForInline::class)
abstract class AbstractFirSpecificAnnotationResolveTransformer(
    @property:PrivateForInline override val session: FirSession,
    @property:PrivateForInline override val scopeSession: ScopeSession,
    @property:PrivateForInline val computationSession: CompilerRequiredAnnotationsComputationSession,
    containingDeclarations: List<FirDeclaration> = emptyList(),
) : FirDefaultTransformer<Nothing?>(), SessionAndScopeSessionHolder {

    private fun resolveCompilerRequiredArguments(
        annotationCall: FirAnnotationCall,
        parameters: List<FirCompilerRequiredParameterDescription>,
    ) {
        val mapping = mutableMapOf<Name, FirExpression>()

        for (parameter in parameters) {
            val arguments = annotationCall.findArgumentsForCompilerRequiredParameter(parameter)
            val value = when (val type = parameter.kind) {
                is FirCraParameterKind.EnumParameter -> resolveEnumArguments(arguments, type)
                is FirCraParameterKind.LiteralParameter -> resolveLiteralArgument(arguments, type.constKind)
            }
            if (value != null) {
                mapping[parameter.name] = value
            }
        }

        annotationCall.replaceArgumentMapping(
            FirAnnotationArgumentMappingImpl(
                annotationCall.argumentMapping.source,
                mapping,
            )
        )
    }

    private fun FirAnnotationCall.findArgumentsForCompilerRequiredParameter(
        parameter: FirCompilerRequiredParameterDescription,
    ): List<FirExpression> {
        val namedArgument = arguments.firstNotNullOfOrNull { argument ->
            (argument as? FirNamedArgumentExpression)?.takeIf { it.name == parameter.name }?.expression
        }

        if (namedArgument != null) return [namedArgument]

        val kind = parameter.kind
        // We rely here on the fact that currently vararg compiler-required parameters are always the only ones
        if (kind is FirCraParameterKind.EnumParameter && kind.isVararg) {
            return arguments
        }

        return parameter.position?.let(arguments::getOrNull)?.takeIf { it !is FirNamedArgumentExpression }.let(::listOfNotNull)
    }

    private fun resolveEnumArguments(
        arguments: List<FirExpression>,
        parameter: FirCraParameterKind.EnumParameter,
    ): FirExpression? {
        if (arguments.isEmpty()) return null

        val entries = arguments.flatMap { it.unwrapAndFlattenArgument(flattenArrays = true) }.map { argument ->
            val symbol = (argument as? FirPropertyAccessExpression)?.let {
                resolvePropertyAccessExpressionFromImports(it, it.calleeReference.name, parameter.enumClassId)
            }

            when (symbol) {
                null -> buildUnresolvedArgumentDuringCompilerRequiredAnnotations(argument.source)
                else -> buildResolvedEnumEntryAccess(argument, symbol)
            }
        }

        if (!parameter.isVararg) {
            return entries.firstOrNull() ?: buildUnresolvedArgumentDuringCompilerRequiredAnnotations(arguments.first().source)
        }

        val elementType = ConeClassLikeTypeImpl(parameter.enumClassId.toLookupTag(), typeArguments = [], isMarkedNullable = false)
        return buildVarargArgumentsExpression {
            this.arguments += entries
            coneElementTypeOrNull = elementType
            coneTypeOrNull = elementType.createOutArrayType()
            source = arguments.first().source
        }
    }

    private fun resolveLiteralArgument(arguments: List<FirExpression>, kind: ConstantValueKind): FirExpression? {
        require(kind is ConstantValueKind.String) {
            "Currently, only string literals can be compiler-required arguments. " +
                    "If you added a new compiler-required parameter with other ${ConstantValueKind::class.simpleName}, " +
                    "this function needs to be updated."
        }
        if (arguments.isEmpty()) return null
        val literal = (arguments.firstOrNull()?.unwrapArgument() as? FirLiteralExpression)?.takeIf { it.kind == ConstantValueKind.String }
            ?: return buildUnresolvedArgumentDuringCompilerRequiredAnnotations(arguments.firstOrNull()?.source)

        return buildLiteralExpression(literal.source, literal.kind, literal.value, setType = true)
    }

    private fun buildResolvedEnumEntryAccess(
        propertyAccess: FirPropertyAccessExpression,
        symbol: FirEnumEntrySymbol,
    ): FirExpression {
        val calleeReference = propertyAccess.calleeReference
        val enumClassLookupTag = symbol.containingClassLookupTag()

        // We deliberately omit receivers here; our goal is just to put some
        // resolved value to the mapping. Expressions from argument mappings should
        // never be asked for their structure, but rather for values they represent.
        return buildPropertyAccessExpression {
            source = propertyAccess.source
            this.calleeReference = buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = symbol
            }
            coneTypeOrNull = enumClassLookupTag?.let { ConeClassLikeTypeImpl(it, typeArguments = [], isMarkedNullable = false) }
        }
    }

    private fun resolvePropertyAccessExpressionFromImports(
        propertyAccess: FirPropertyAccessExpression,
        name: Name,
        expectedEnumClassId: ClassId,
    ): FirEnumEntrySymbol? {
        fun guessEnumEntryByName(): FirEnumEntrySymbol? {
            return (expectedEnumClassId.toSymbol() as? FirRegularClassSymbol)?.declarationSymbols?.firstNotNullOfOrNull { declaration ->
                (declaration as? FirEnumEntrySymbol)?.takeIf { it.name == name }
            }
        }

        // with explicit receiver, it is impossible that enum entry was renamed through import
        if (propertyAccess.explicitReceiver != null) return guessEnumEntryByName()

        @OptIn(FirImplementationDetail::class)
        val fromImports = scopes.firstNotNullOfOrNull { scope ->
            (scope as? FirAbstractImportingScope)?.findEnumEntryWithoutResolution(name)
                ?.takeIf { it.containingClassLookupTag()?.classId == expectedEnumClassId }
        }

        return fromImports ?: guessEnumEntryByName()
    }

    private fun buildUnresolvedArgumentDuringCompilerRequiredAnnotations(source: KtSourceElement?): FirErrorExpression =
        buildErrorExpression(source, ConeUnresolvedArgumentDuringCompilerRequiredAnnotations)

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

        val annotationClassId = transformedAnnotationType.coneType.classLikeLookupTagIfAny?.classId
        val compilerRequiredParameters = annotationClassId?.let { session.annotationPlatformSupport.requiredAnnotationsWithArguments[it] }
        if (compilerRequiredParameters != null) {
            resolveCompilerRequiredArguments(annotationCall, compilerRequiredParameters)
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
        if (metaAnnotationsFromPlugins.isNotEmpty()) return true
        val name = typeRef.shortName
        if (name.isPotentiallyCompilerRequiredAnnotationName()) return true

        val originalName = aliasedImports[name] ?: return false
        return originalName.isPotentiallyCompilerRequiredAnnotationName()
    }

    private fun Name.isPotentiallyCompilerRequiredAnnotationName(): Boolean {
        return this in session.annotationPlatformSupport.requiredAnnotationsShortClassNames ||
                annotationsFromPlugins.any { it.shortName() == this }
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
        resolveClass(
            regularClass,
            transformChildren = {
                regularClass.transformDeclarations(this, data)
            },
        )

        return regularClass
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: Nothing?,
    ): FirStatement {
        resolveClass(
            anonymousObject,
            transformChildren = {
                anonymousObject.transformDeclarations(this, data)
            }
        )
        return anonymousObject
    }

    inline fun resolveClass(
        klass: FirClass,
        transformChildren: () -> Unit,
    ) {
        withClass(klass) {
            if (!shouldTransformDeclaration(klass)) return
            if (!computationSession.annotationResolutionWasAlreadyStarted(klass)) {
                computationSession.recordThatAnnotationResolutionStarted(klass)
                transformDeclaration(klass, null)
                computationSession.recordThatAnnotationsAreResolved(klass)
            }

            transformChildren(klass) {
                if (klass is FirRegularClass) {
                    klass.transformContextParameters(this, null)
                }
                transformChildren()
            }

            calculateDeprecations(klass)
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

    inline fun resolveReplSnippet(
        replSnippet: FirReplSnippet,
        block: () -> Unit,
    ) {
        if (!shouldTransformDeclaration(replSnippet)) return

        computationSession.recordThatAnnotationsAreResolved(replSnippet)
        transformDeclaration(replSnippet, data = null)
        transformChildren(replSnippet) {
            block()
        }
    }

    override fun transformReplSnippet(replSnippet: FirReplSnippet, data: Nothing?): FirReplSnippet {
        resolveReplSnippet(replSnippet) {
            replSnippet.transformChildren(this, data)
        }

        return replSnippet
    }

    inline fun withClass(
        klass: FirClass,
        action: () -> Unit
    ) {
        withClassDeclarationCleanup(classDeclarationsStack, klass) {
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
            withFileScopesAndImports(file) {
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
            withFileAnalysisExceptionWrapping(file, f)
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
    lateinit var aliasedImports: Map<Name, Name>

    inline fun <T> withFileScopesAndImports(file: FirFile, f: () -> T): T {
        scopes = createImportingScopes(file, session, scopeSession, useCaching = computationSession.useCacheForImportScope)
        aliasedImports = buildMap {
            for (import in file.imports) {
                val importedName = import.importedFqName?.shortName() ?: continue
                val aliasName = import.aliasName ?: continue
                put(aliasName, importedName)
            }
        }
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

    override fun transformNamedFunction(
        namedFunction: FirNamedFunction,
        data: Nothing?
    ): FirNamedFunction = transformFunctionDeclarationForDeprecations(namedFunction, data)

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
        owners = owners.adding(parentDeclaration)
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
