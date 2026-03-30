/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.declarations.utils.isFromVararg
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentListCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCallCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCallCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildInaccessibleReceiverExpressionCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpressionCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpressionCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildSmartCastExpressionCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildThisReceiverExpressionCopy
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.replSnippetResolveExtensions
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.TypeResolutionConfiguration
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguouslyResolvedAnnotationFromPlugin
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCyclicTypeBound
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConePackFunctionAmbiguity
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.removeParameterNameAnnotation
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getClassifiers
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.valueParameterTypesWithoutReceivers
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirTypeResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.TYPES) {
    override val transformer: FirTypeResolveTransformer = FirTypeResolveTransformer(session, scopeSession)
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

@OptIn(PrivateForInline::class)
open class FirTypeResolveTransformer(
    final override val session: FirSession,
    @property:PrivateForInline val scopeSession: ScopeSession,
    initialScopes: List<FirScope> = emptyList(),
    initialCurrentFile: FirFile? = null,
    @property:PrivateForInline val classDeclarationsStack: ArrayDeque<FirClass> = ArrayDeque()
) : FirAbstractTreeTransformer<Any?>(FirResolvePhase.TYPES) {
    private val packExpansionInProgress: MutableSet<FirNamedFunctionSymbol> = mutableSetOf()
    private val packExpansionFinished: MutableSet<FirNamedFunctionSymbol> = mutableSetOf()

    /**
     * All current scopes sorted from outermost to innermost.
     */
    @PrivateForInline
    var scopes: PersistentList<FirScope> = initialScopes.asReversed().toPersistentList()

    /**
     * Scopes that are accessible statically, i.e. [scopes] minus type parameter scopes.
     */
    @PrivateForInline
    var staticScopes: PersistentList<FirScope> = scopes

    @set:PrivateForInline
    var scopesBefore: PersistentList<FirScope>? = null

    @set:PrivateForInline
    var staticScopesBefore: PersistentList<FirScope>? = null

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

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session, expandTypeAliases = true)

    @PrivateForInline
    var currentFile: FirFile? = initialCurrentFile

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        return withFileScope(file) {
            super.transformFile(file, data)
        }
    }

    inline fun <R> withFileScope(file: FirFile, crossinline action: () -> R): R {
        currentFile = file
        return withScopeCleanup {
            addScopes(createImportingScopes(file, session, scopeSession))
            action()
        }
    }

    override fun transformReplSnippet(replSnippet: FirReplSnippet, data: Any?): FirReplSnippet {
        whileAnalysing(session, replSnippet) {
            return withReplSnippetScope(replSnippet) {
                transformElement(replSnippet, data)
            }
        }
    }

    inline fun <R> withReplSnippetScope(replSnippet: FirReplSnippet, crossinline action: () -> R): R {
        return withScopeCleanup {
            addScopes(buildList {
                // TODO: robuster matching and error reporting on no extension (KT-72969)
                for (resolveExt in session.extensionService.replSnippetResolveExtensions) {
                    val scope = resolveExt.getSnippetScope(replSnippet, session)
                    if (scope != null) add(scope)
                }
            })
            action()
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        whileAnalysing(session, regularClass) {
            withClassDeclarationCleanup(regularClass) {
                transformClassTypeParameters(regularClass, data)
                return resolveClassContent(regularClass, data)
            }
        }
    }

    fun transformClassTypeParameters(regularClass: FirRegularClass, data: Any?) {
        withScopeCleanup {
            // Remove type parameter scopes for classes that are neither inner nor local
            if (removeOuterTypeParameterScope(regularClass)) {
                this.scopes = staticScopes
            }
            addTypeParametersScope(regularClass)
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
            unboundCyclesInTypeParametersSupertypes(regularClass)
        }
    }

    inline fun <R> withClassDeclarationCleanup(regularClass: FirRegularClass, action: () -> R): R {
        return withClassDeclarationCleanup(classDeclarationsStack, regularClass, action)
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        withClassDeclarationCleanup(classDeclarationsStack, anonymousObject) {
            return resolveClassContent(anonymousObject, data)
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: Any?): FirConstructor = whileAnalysing(session, constructor) {
        return withScopeCleanup {
            addTypeParametersScope(constructor)
            val result = transformDeclaration(constructor, data) as FirConstructor

            if (result.isPrimary) {
                val shouldAddDefaultStubs = session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) &&
                        session.moduleData.isCommon &&
                        constructor.returnTypeRef.coneType.classId == StandardClassIds.Enum
                for (valueParameter in result.valueParameters) {
                    if (valueParameter.correspondingProperty != null) {
                        valueParameter.moveOrDeleteIrrelevantAnnotations()
                    }
                    if (shouldAddDefaultStubs) {
                        valueParameter.replaceDefaultValue(buildExpressionStub()) // TODO: Remove when KT-67381 is implemented
                    }
                }
            }

            result
        }
    }

    override fun transformAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?): FirAnonymousInitializer {
        return withScopeCleanup {
            transformDeclaration(anonymousInitializer, data) as FirAnonymousInitializer
        }
    }

    override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: Any?): FirConstructor =
        transformConstructor(errorPrimaryConstructor, data)

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirTypeAlias = whileAnalysing(session, typeAlias) {
        withScopeCleanup {
            addTypeParametersScope(typeAlias)
            transformDeclaration(typeAlias, data)
        } as FirTypeAlias
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Any?): FirEnumEntry = whileAnalysing(session, enumEntry) {
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        enumEntry
    }

    override fun transformReceiverParameter(receiverParameter: FirReceiverParameter, data: Any?): FirReceiverParameter {
        val transformedAnnotations = receiverParameter.transformAnnotations(this, data)
        return typeResolverTransformer.withBareTypes(receiverParameter.containingDeclarationSymbol.let { it is FirCallableSymbol && it.isCompanionExtension }) {
            transformedAnnotations.transformTypeRef(this, data)
        }
    }

    override fun transformProperty(property: FirProperty, data: Any?): FirProperty = whileAnalysing(session, property) {
        withScopeCleanup {
            if (property.isStatic) {
                scopes = staticScopes
            }

            withDeclaration(property) {
                addTypeParametersScope(property)
                property.transformTypeParameters(this, data)
                    .transformReturnTypeRef(this, data)
                    .transformReceiverParameter(this, data)
                    .transformContextParameters(this, data)
                    .transformGetter(this, data)
                    .transformSetter(this, data)
                    .transformBackingField(this, data)
                    .transformAnnotations(this, data)

                if (property.isFromVararg == true) {
                    property.transformTypeToArrayType(session)
                    property.backingField?.transformTypeToArrayType(session)
                    setAccessorTypesByPropertyType(property)
                }

                when {
                    property.returnTypeRef is FirResolvedTypeRef && property.delegate != null -> {
                        setAccessorTypesByPropertyType(property)
                    }
                    property.returnTypeRef !is FirResolvedTypeRef && property.initializer == null &&
                            property.getter?.returnTypeRef is FirResolvedTypeRef -> {
                        val returnTypeRef = property.getter!!.returnTypeRef

                        property.replaceReturnTypeRef(returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType))
                        property.backingField?.replaceReturnTypeRef(
                            returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType)
                        )

                        property.setter?.valueParameters?.forEach {
                            it.replaceReturnTypeRef(
                                returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyTypeFromGetterReturnType)
                            )
                        }
                    }
                }

                unboundCyclesInTypeParametersSupertypes(property)

                property.moveOrDeleteIrrelevantAnnotations()
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
            field
        }
    }

    override fun transformBackingField(backingField: FirBackingField, data: Any?): FirStatement = whileAnalysing(session, backingField) {
        backingField.transformAnnotations(this, data)
        super.transformBackingField(backingField, data)
    }

    override fun transformNamedFunction(
        namedFunction: FirNamedFunction,
        data: Any?,
    ): FirNamedFunction = whileAnalysing(session, namedFunction) {
        withScopeCleanup {
            if (namedFunction.isStatic) {
                scopes = staticScopes
            }

            withDeclaration(namedFunction) {
                addTypeParametersScope(namedFunction)
                expandPackParametersIfNeeded(namedFunction)
                val result = transformDeclaration(namedFunction, data).also {
                    unboundCyclesInTypeParametersSupertypes(it as FirTypeParametersOwner)
                }

                if (result.source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers &&
                    result is FirNamedFunction &&
                    result.name == StandardNames.DATA_CLASS_COPY
                ) {
                    for (valueParameter in result.valueParameters) {
                        valueParameter.moveOrDeleteIrrelevantAnnotations()
                    }
                }

                result
            }
        } as FirNamedFunction
    }

    private fun expandPackParametersIfNeeded(function: FirNamedFunction) {
        if (!packExpansionFinished.add(function.symbol)) {
            return
        }

        val originalParameters = function.valueParameters
        if (originalParameters.none { it.packSelector() != null }) {
            return
        }

        if (!packExpansionInProgress.add(function.symbol)) {
            return
        }

        try {
            val expandedParameters = mutableListOf<FirValueParameter>()
            var changed = false

            for (parameter in originalParameters) {
                val packSelector = parameter.packSelector() ?: run {
                    expandedParameters += parameter
                    continue
                }

                val replacementParameters = expandPackParameter(function, parameter, packSelector)
                if (replacementParameters == null) {
                    expandedParameters += parameter
                    continue
                }

                changed = true
                expandedParameters += replacementParameters
            }

            if (changed) {
                function.replaceValueParameters(expandedParameters)
            }
        } finally {
            packExpansionInProgress.remove(function.symbol)
        }
    }

    private fun expandPackParameter(
        owner: FirNamedFunction,
        packParameter: FirValueParameter,
        packSelector: PackSelector,
    ): List<FirValueParameter>? {
        resolveClassifier(packSelector.receiverName)?.let { classifierSymbol ->
            if (packSelector.kind == PackSelectorKind.SharedProps) {
                return packParameter.reportPackExpansionError(
                    ConeUnsupported(
                        "Type pack selector ${packSelector.render()} is only supported for function overload sets.",
                        packParameter.source
                    )
                )
            }

            if (packSelector.overloadSelectorName != null) {
                return packParameter.reportPackExpansionError(
                    ConeUnsupported(
                        "Type pack selector ${packSelector.render()} does not support overload selectors.",
                        packParameter.source
                    )
                )
            }

            val sourceType = classifierSymbol.toPackSourceType() ?: return null
            val properties = collectPackProperties(sourceType)
                .filter { packSelector.kind.matchesPackProjection(it.returnTypeRef) }
            return properties.map { property ->
                buildPackValueParameter(
                    owner,
                    packParameter,
                    property.name,
                    property.returnTypeRef,
                    packDefaultValue = property.packDefaultValue()
                )
            }
        }

        val targetFunctions = resolveFunctions(packSelector.receiverName)
        if (targetFunctions.isEmpty()) {
            return expandPackVariableParameter(owner, packParameter, packSelector)
        }

        val targetParameters = when (packSelector.kind) {
            PackSelectorKind.Props,
            PackSelectorKind.Attrs,
            PackSelectorKind.Callbacks,
            PackSelectorKind.Slots -> {
                val targetFunctionSymbol = when {
                    packSelector.overloadSelectorName != null ->
                        selectPackFunctionOverload(targetFunctions, packSelector.overloadSelectorName, packParameter)
                    targetFunctions.size == 1 -> targetFunctions.single()
                    else -> {
                        return packParameter.reportPackExpansionError(
                            ConePackFunctionAmbiguity(packSelector.receiverName, targetFunctions)
                        )
                    }
                } ?: return null

                resolveExpandedFunctionParameters(targetFunctionSymbol)
                    ?.filter { packSelector.kind.matchesPackProjection(it.returnTypeRef) }
                    ?: return null
            }
            PackSelectorKind.SharedProps -> {
                buildSharedFunctionParameters(owner, packParameter, packSelector, targetFunctions) ?: return null
            }
        }

        return targetParameters.map { targetParameter ->
            buildPackValueParameter(
                owner,
                packParameter,
                targetParameter.name,
                targetParameter.returnTypeRef,
                packDefaultValue = null,
            )
        }
    }

    private fun selectPackFunctionOverload(
        functionSymbols: List<FirNamedFunctionSymbol>,
        overloadSelectorName: Name,
        packParameter: FirValueParameter,
    ): FirNamedFunctionSymbol? {
        val selectorVariable = resolveVariable(overloadSelectorName)
            ?: return packParameter.reportPackExpansionError(
                ConeUnsupported(
                    "Pack overload selector ${overloadSelectorName.asString()} must resolve to a function-typed value.",
                    packParameter.source
                )
            )
        val selectorShape = selectorVariable.functionTypeShape()
            ?: return packParameter.reportPackExpansionError(
                ConeUnsupported(
                    "Pack overload selector ${overloadSelectorName.asString()} must have an explicit function type.",
                    packParameter.source
                )
            )

        val matchingSymbols = functionSymbols.filter { symbol ->
            val parameters = resolveExpandedFunctionParameters(symbol) ?: return@filter false
                    parameters.size == selectorShape.parameterTypes.size &&
                    parameters.zip(selectorShape.parameterTypes).all { (parameter, selectorType) ->
                        val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return@all false
                        parameterType.equalPackSelectorOverloadType(selectorType)
                    }
        }

        return when (matchingSymbols.size) {
            0 -> packParameter.reportPackExpansionError(
                ConeUnsupported(
                    "No overload of ${packParameter.packSelector()?.receiverName?.asString()} matches selector ${overloadSelectorName.asString()}.",
                    packParameter.source
                )
            )
            1 -> matchingSymbols.single()
            else -> packParameter.reportPackExpansionError(
                ConePackFunctionAmbiguity(packParameter.packSelector()!!.receiverName, matchingSymbols)
            )
        }
    }

    private fun buildSharedFunctionParameters(
        owner: FirNamedFunction,
        packParameter: FirValueParameter,
        packSelector: PackSelector,
        functionSymbols: List<FirNamedFunctionSymbol>,
    ): List<FirValueParameter>? {
        val expandedSignatures = functionSymbols.map { symbol ->
            symbol to (resolveExpandedFunctionParameters(symbol) ?: return null)
        }
        val anchorParameters = expandedSignatures.firstOrNull()?.second ?: return null
        val sharedParameters = anchorParameters.mapNotNull { anchorParameter ->
            val matchingParameters = expandedSignatures.mapNotNull { (_, parameters) ->
                parameters.find { it.name == anchorParameter.name }
            }
            if (matchingParameters.size != expandedSignatures.size) {
                return@mapNotNull null
            }

            val anchorType = anchorParameter.returnTypeRef.coneTypeOrNull ?: return@mapNotNull null
            val hasDifferentType = matchingParameters.drop(1).any { parameter ->
                val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return@any true
                !anchorType.equalTypes(parameterType, session)
            }
            if (hasDifferentType) {
                return@mapNotNull null
            }

            buildPackValueParameter(
                owner,
                packParameter,
                anchorParameter.name,
                anchorParameter.returnTypeRef,
                packDefaultValue = null,
            )
        }

        if (sharedParameters.isEmpty()) {
            return packParameter.reportPackExpansionError(
                ConeUnsupported(
                    "Function pack selector ${packSelector.render()} has no shared parameters across overloads.",
                    packParameter.source
                )
            )
        }

        return sharedParameters
    }

    private fun resolveExpandedFunctionParameters(functionSymbol: FirNamedFunctionSymbol): List<FirValueParameter>? {
        val targetFunction = functionSymbol.fir
        val requiresPackExpansion = targetFunction.valueParameters.any { it.packSelector() != null }
        val hasUnresolvedParameterTypes = targetFunction.valueParameters.any { it.returnTypeRef !is FirResolvedTypeRef }
        if (requiresPackExpansion || hasUnresolvedParameterTypes) {
            if (functionSymbol in packExpansionInProgress) {
                return null
            }

            withScopeCleanup {
                withFunctionResolutionScope(functionSymbol) {
                    withDeclaration(targetFunction) {
                        addTypeParametersScope(targetFunction)
                        if (requiresPackExpansion) {
                            expandPackParametersIfNeeded(targetFunction)
                        }
                        targetFunction.transformValueParameters(this@FirTypeResolveTransformer, null)
                    }
                }
            }
        }

        return targetFunction.valueParameters
    }

    private inline fun <T> withFunctionResolutionScope(functionSymbol: FirNamedFunctionSymbol, crossinline action: () -> T): T {
        return withCallableResolutionScope(functionSymbol, action)
    }

    private inline fun <T> withCallableResolutionScope(symbol: FirCallableSymbol<*>, crossinline action: () -> T): T {
        val targetSession = symbol.moduleData.session
        if (targetSession === session) {
            val targetFile = targetSession.firProvider.getFirCallableContainerFile(symbol)
            if (targetFile != null) {
                return withFileScope(targetFile, action)
            }
        }

        return action()
    }

    private fun expandPackVariableParameter(
        owner: FirNamedFunction,
        packParameter: FirValueParameter,
        packSelector: PackSelector,
    ): List<FirValueParameter>? {
        if (packSelector.overloadSelectorName != null) {
            return packParameter.reportPackExpansionError(
                ConeUnsupported(
                    "Value pack selector ${packSelector.render()} does not support overload selectors.",
                    packParameter.source
                )
            )
        }

        val variableSymbol = resolveVariable(packSelector.receiverName) ?: return null
        val functionShape = variableSymbol.functionTypeShape() ?: return null
        return functionShape.parameterNames.indices
            .filter { index ->
                packSelector.kind.matchesPackProjection(
                    functionShape.parameterTypes[index],
                    functionShape.parameterSourceTexts?.getOrNull(index),
                )
            }
            .map { index ->
                buildPackValueParameter(
                    owner,
                    packParameter,
                    functionShape.parameterNames[index],
                    functionShape.parameterTypes[index].toFirResolvedTypeRef(),
                    packDefaultValue = null,
                )
            }
    }

    private fun collectPackProperties(type: ConeKotlinType): List<FirProperty> {
        val orderedProperties = LinkedHashMap<Name, FirProperty>()
        collectPackProperties(type, orderedProperties, mutableSetOf<FirClassLikeSymbol<*>>())
        return orderedProperties.values.toList()
    }

    private fun collectPackProperties(
        type: ConeKotlinType,
        orderedProperties: LinkedHashMap<Name, FirProperty>,
        visitedClasses: MutableSet<FirClassLikeSymbol<*>>,
    ) {
        when (type) {
            is ConeClassLikeType -> {
                val classSymbol = type.lookupTag.toClassSymbol(session) ?: return
                if (!visitedClasses.add(classSymbol)) {
                    return
                }

                classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                val klass = classSymbol.fir
                for (superType in klass.superConeTypes) {
                    collectPackProperties(superType, orderedProperties, visitedClasses)
                }
                for (declaration in klass.declarations) {
                    val property = declaration as? FirProperty ?: continue
                    if (property.receiverParameter != null || property.isStatic) {
                        continue
                    }
                    orderedProperties[property.name] = property
                }
            }

            is ConeTypeParameterType -> {
                val symbol = type.lookupTag.symbol
                symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
                symbol.resolvedBounds.forEach { bound ->
                    collectPackProperties(bound.coneType, orderedProperties, visitedClasses)
                }
            }

            is ConeIntersectionType -> {
                type.intersectedTypes.forEach { intersectedType ->
                    collectPackProperties(intersectedType, orderedProperties, visitedClasses)
                }
            }

            is ConeFlexibleType -> {
                collectPackProperties(type.lowerBound, orderedProperties, visitedClasses)
            }

            is ConeDefinitelyNotNullType -> {
                collectPackProperties(type.original, orderedProperties, visitedClasses)
            }

            is ConeCapturedType -> {
                type.constructor.supertypes?.forEach { superType ->
                    collectPackProperties(superType, orderedProperties, visitedClasses)
                }
            }

            else -> {}
        }
    }

    private fun buildPackValueParameter(
        owner: FirNamedFunction,
        originalPackParameter: FirValueParameter,
        name: Name,
        returnTypeRef: FirTypeRef,
        packDefaultValue: FirExpression?,
    ): FirValueParameter {
        return buildValueParameter {
            source = originalPackParameter.source?.fakeElement(KtFakeSourceElementKind.PackExpandedValueParameter)
            moduleData = owner.moduleData
            origin = FirDeclarationOrigin.Source
            this.returnTypeRef = returnTypeRef
            this.name = name
            symbol = FirValueParameterSymbol()
            containingDeclarationSymbol = owner.symbol
            defaultValue = packDefaultValue
        }
    }

    private fun FirProperty.packDefaultValue(): FirExpression? {
        val constructorParameter = correspondingValueParameterFromPrimaryConstructor?.fir
        if (constructorParameter != null) {
            return constructorParameter.packDefaultValue()
        }

        return initializer?.clonePackDefaultValue()
    }

    private fun FirValueParameter.packDefaultValue(): FirExpression? {
        return defaultValue.clonePackDefaultValue()
    }

    private fun FirExpression?.clonePackDefaultValue(): FirExpression? {
        val defaultValue = this ?: return null
        if (defaultValue is org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub) {
            return null
        }

        return defaultValue.copyPackDefaultExpression()
    }

    @OptIn(FirIdeOnly::class, UnresolvedExpressionTypeAccess::class)
    private fun FirExpression.copyPackDefaultExpression(): FirExpression? {
        return when (this) {
            is FirLiteralExpression -> buildLiteralExpression(
                source = source,
                kind = kind,
                value = value,
                annotations = annotations.takeIf { it.isNotEmpty() }?.toMutableList(),
                setType = false,
                prefix = prefix,
            ).apply {
                this@copyPackDefaultExpression.coneTypeOrNull?.let(::replaceConeTypeOrNull)
            }

            is FirResolvedQualifier -> buildResolvedQualifier {
                source = this@copyPackDefaultExpression.source
                contextSensitiveAlternative =
                    this@copyPackDefaultExpression.contextSensitiveAlternative?.copyPackDefaultExpression() as? FirPropertyAccessExpression
                coneTypeOrNull = this@copyPackDefaultExpression.coneTypeOrNull
                annotations += this@copyPackDefaultExpression.annotations
                packageFqName = this@copyPackDefaultExpression.packageFqName
                relativeClassFqName = this@copyPackDefaultExpression.relativeClassFqName
                symbol = this@copyPackDefaultExpression.symbol
                explicitParent = this@copyPackDefaultExpression.explicitParent?.copyPackDefaultExpression() as? FirResolvedQualifier
                isNullableLHSForCallableReference = this@copyPackDefaultExpression.isNullableLHSForCallableReference
                resolvedLHSTypeForCallableReferenceOrNull = this@copyPackDefaultExpression.resolvedLHSTypeForCallableReferenceOrNull
                resolvedToCompanionObject = this@copyPackDefaultExpression.resolvedToCompanionObject
                canBeValue = this@copyPackDefaultExpression.canBeValue
                isFullyQualified = this@copyPackDefaultExpression.isFullyQualified
                nonFatalDiagnostics += this@copyPackDefaultExpression.nonFatalDiagnostics
                resolvedSymbolOrigin = this@copyPackDefaultExpression.resolvedSymbolOrigin
                typeArguments += this@copyPackDefaultExpression.typeArguments
            }

            is FirPropertyAccessExpression -> buildPropertyAccessExpressionCopy(this) {
                explicitReceiver = this@copyPackDefaultExpression.explicitReceiver?.copyPackDefaultExpression()
                dispatchReceiver = this@copyPackDefaultExpression.dispatchReceiver?.copyPackDefaultExpression()
                extensionReceiver = this@copyPackDefaultExpression.extensionReceiver?.copyPackDefaultExpression()
                contextArguments.clear()
                contextArguments += this@copyPackDefaultExpression.contextArguments.mapNotNull { it.copyPackDefaultExpression() }
                calleeReference = this@copyPackDefaultExpression.calleeReference.copyPackDefaultReference(source)
                contextSensitiveAlternative =
                    this@copyPackDefaultExpression.contextSensitiveAlternative?.copyPackDefaultExpression() as? FirPropertyAccessExpression
            }

            is FirFunctionCall -> buildFunctionCallCopy(this) {
                explicitReceiver = this@copyPackDefaultExpression.explicitReceiver?.copyPackDefaultExpression()
                dispatchReceiver = this@copyPackDefaultExpression.dispatchReceiver?.copyPackDefaultExpression()
                extensionReceiver = this@copyPackDefaultExpression.extensionReceiver?.copyPackDefaultExpression()
                contextArguments.clear()
                contextArguments += this@copyPackDefaultExpression.contextArguments.mapNotNull { it.copyPackDefaultExpression() }
                argumentList = buildArgumentListCopy(this@copyPackDefaultExpression.argumentList) {
                    arguments.clear()
                    arguments += this@copyPackDefaultExpression.argumentList.arguments.mapNotNull { it.copyPackDefaultExpression() }
                }
                calleeReference = this@copyPackDefaultExpression.calleeReference.copyPackDefaultReference(source)
            }

            is FirNamedArgumentExpression -> {
                val copiedExpression = this.expression.copyPackDefaultExpression() ?: return null
                buildNamedArgumentExpressionCopy(this) {
                    expression = copiedExpression
                }
            }

            is FirStringConcatenationCall -> buildStringConcatenationCall {
                source = this@copyPackDefaultExpression.source
                annotations += this@copyPackDefaultExpression.annotations
                interpolationPrefix = this@copyPackDefaultExpression.interpolationPrefix
                isFoldedStrings = this@copyPackDefaultExpression.isFoldedStrings
                argumentList = buildArgumentListCopy(this@copyPackDefaultExpression.argumentList) {
                    arguments.clear()
                    arguments += this@copyPackDefaultExpression.argumentList.arguments.mapNotNull { it.copyPackDefaultExpression() }
                }
            }

            is FirThisReceiverExpression -> buildThisReceiverExpressionCopy(this) {}
            is FirInaccessibleReceiverExpression -> buildInaccessibleReceiverExpressionCopy(this) {}
            is FirSmartCastExpression -> {
                val copiedOriginalExpression = this.originalExpression.copyPackDefaultExpression() ?: return null
                buildSmartCastExpressionCopy(this) {
                    originalExpression = copiedOriginalExpression
                    smartcastType = this@copyPackDefaultExpression.smartcastType
                }
            }

            else -> null
        }
    }

    private fun FirNamedReference.copyPackDefaultReference(newSource: KtSourceElement?): FirNamedReference {
        return when (this) {
            is FirResolvedNamedReference -> buildResolvedNamedReference {
                source = newSource
                name = this@copyPackDefaultReference.name
                resolvedSymbol = this@copyPackDefaultReference.resolvedSymbol
            }

            is FirNamedReferenceWithCandidate -> FirNamedReferenceWithCandidate(
                newSource,
                this@copyPackDefaultReference.name,
                candidate = this@copyPackDefaultReference.candidate,
            )

            is FirSimpleNamedReference -> buildSimpleNamedReference {
                source = newSource
                name = this@copyPackDefaultReference.name
            }

            is FirErrorNamedReference -> buildErrorNamedReference {
                source = newSource
                name = this@copyPackDefaultReference.name
                diagnostic = this@copyPackDefaultReference.diagnostic
            }

            else -> this
        }
    }

    private fun FirValueParameter.packSelector(): PackSelector? = source.parsePackSelector()

    private fun resolveClassifier(name: Name): FirClassifierSymbol<*>? {
        for (scope in scopes.asReversed()) {
            val classifier = scope.getClassifiers(name).firstOrNull() ?: continue
            return classifier
        }
        return null
    }

    private fun resolveFunctions(name: Name): List<FirNamedFunctionSymbol> {
        for (scope in scopes.asReversed()) {
            val functions = scope.getFunctions(name).distinct().toList()
            if (functions.isEmpty()) {
                continue
            }
            return functions
        }
        return emptyList()
    }

    private fun resolveVariable(name: Name): FirVariableSymbol<*>? {
        for (scope in scopes.asReversed()) {
            val properties = scope.getProperties(name).distinct().toList()
            if (properties.isEmpty()) {
                continue
            }
            if (properties.size == 1) {
                return properties.single()
            }
            return null
        }
        return null
    }

    private fun KtSourceElement?.extractNamedFunctionTypeParameters(): List<PackFunctionTypeParameterSource>? {
        return text?.toString().extractNamedFunctionTypeParameters()
    }

    private fun String?.extractNamedFunctionTypeParameters(): List<PackFunctionTypeParameterSource>? {
        val normalizedText = this
            ?.filterNot(Char::isWhitespace)
            ?: return null
        if (!normalizedText.startsWith("(")) {
            return null
        }

        val arrowIndex = normalizedText.indexOf(")->")
        if (arrowIndex < 0) {
            return null
        }

        val parameterBlock = normalizedText.substring(1, arrowIndex)
        if (parameterBlock.isEmpty()) {
            return emptyList()
        }

        return splitTopLevel(parameterBlock).map { parameterText ->
            val colonIndex = parameterText.indexOf(':')
            if (colonIndex <= 0) {
                return null
            }

            val name = parameterText.substring(0, colonIndex).removeSurrounding("`")
            if (name.isEmpty()) {
                return null
            }

            PackFunctionTypeParameterSource(
                name = Name.identifier(name),
                typeText = parameterText.substring(colonIndex + 1),
            )
        }
    }

    private fun splitTopLevel(text: String): List<String> {
        val result = mutableListOf<String>()
        var startIndex = 0
        var angleDepth = 0
        var roundDepth = 0
        var squareDepth = 0

        for ((index, char) in text.withIndex()) {
            when (char) {
                '<' -> angleDepth++
                '>' -> angleDepth--
                '(' -> roundDepth++
                ')' -> roundDepth--
                '[' -> squareDepth++
                ']' -> squareDepth--
                ',' -> {
                    if (angleDepth == 0 && roundDepth == 0 && squareDepth == 0) {
                        result += text.substring(startIndex, index)
                        startIndex = index + 1
                    }
                }
            }
        }

        result += text.substring(startIndex)
        return result
    }

    private fun KtSourceElement?.parsePackSelector(): PackSelector? {
        val normalizedText = text
            ?.toString()
            ?.filterNot(Char::isWhitespace)
            ?: return null
        val spreadPrefixIndex = normalizedText.indexOf("...")
        if (spreadPrefixIndex < 0) {
            return null
        }

        return normalizedText.substring(spreadPrefixIndex + 3).parsePackSelectorText()
    }

    private fun String.parsePackSelectorText(): PackSelector? {
        val dotIndex = indexOf(".$")
        if (dotIndex <= 0) {
            return null
        }

        val receiverName = substring(0, dotIndex).parsePackSelectorName() ?: return null
        val selectorText = substring(dotIndex + 1)
        PackSelectorKind.entries.firstOrNull { selectorText == it.selectorText }?.let { selectorKind ->
            return PackSelector(receiverName, selectorKind)
        }

        return PackSelectorKind.entries.firstNotNullOfOrNull { selectorKind ->
            if (!selectorKind.supportsOverloadSelector) {
                return@firstNotNullOfOrNull null
            }

            val selectorPrefix = "${selectorKind.selectorText}("
            if (!selectorText.startsWith(selectorPrefix) || !selectorText.endsWith(")")) {
                return@firstNotNullOfOrNull null
            }

            val overloadSelectorName = selectorText
                .removePrefix(selectorPrefix)
                .removeSuffix(")")
                .parsePackSelectorName()
                ?: return@firstNotNullOfOrNull null
            PackSelector(receiverName, selectorKind, overloadSelectorName)
        }
    }

    private fun String.parsePackSelectorName(): Name? {
        val normalizedName = removeSurrounding("`")
        if (normalizedName.isEmpty() || '.' in normalizedName || '(' in normalizedName || ')' in normalizedName) {
            return null
        }

        return Name.identifier(normalizedName)
    }

    private fun FirTypeRef.resolvePackTypeRef(): FirResolvedTypeRef? {
        return when (this) {
            is FirResolvedTypeRef -> this
            else -> transformTypeRef(this, null)
        }
    }

    private fun FirVariableSymbol<*>.functionTypeShape(): PackFunctionShape? {
        val resolvedTypeRef = withCallableResolutionScope(this) {
            fir.returnTypeRef.resolvePackTypeRef()
        } ?: return null
        val coneType = resolvedTypeRef.coneType.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null
        if (!coneType.isSomeFunctionType(session)) {
            return null
        }

        val parameterTypes = coneType.valueParameterTypesWithoutReceivers(session)
        val sourceParameters = fir.returnTypeRef.source.extractNamedFunctionTypeParameters()
        val parameterNames = parameterTypes.map { it.valueParameterName(session) }
            .takeIf { names -> names.all { it != null } }
            ?.map { it!! }
            ?: sourceParameters?.map { it.name }
            ?: return null

        if (parameterNames.size != parameterTypes.size) {
            return null
        }

        val parameterSourceTexts = sourceParameters
            ?.takeIf { it.size == parameterTypes.size }
            ?.map { it.typeText }

        return PackFunctionShape(parameterNames, parameterTypes, parameterSourceTexts)
    }

    private fun ConeKotlinType.equalPackSelectorOverloadType(other: ConeKotlinType): Boolean {
        return normalizePackSelectorOverloadType().equalTypes(other.normalizePackSelectorOverloadType(), session)
    }

    private fun ConeKotlinType.normalizePackSelectorOverloadType(): ConeKotlinType {
        val normalizedArguments = typeArguments.takeIf { it.isNotEmpty() }?.map { projection ->
            val projectionType = projection.type ?: return@map projection
            projection.replaceType(projectionType.normalizePackSelectorOverloadType())
        }?.toTypedArray()

        var normalizedType = if (normalizedArguments != null) {
            withArguments(normalizedArguments)
        } else {
            this
        }

        normalizedType = normalizedType.removeParameterNameAnnotation()
        normalizedType = normalizedType.removeAnnotations()
        return normalizedType
    }

    private fun FirValueParameter.reportPackExpansionError(diagnostic: ConeDiagnostic): Nothing? {
        replaceReturnTypeRef(
            buildErrorTypeRef {
                source = this@reportPackExpansionError.source
                this.diagnostic = diagnostic
            }
        )
        return null
    }

    private fun FirClassifierSymbol<*>.toPackSourceType(): ConeKotlinType? {
        return when (this) {
            is FirTypeParameterSymbol -> defaultType
            is FirClassLikeSymbol<*> -> defaultType()
        }
    }

    private fun PackSelectorKind.matchesPackProjection(typeRef: FirTypeRef): Boolean {
        val coneType = typeRef.resolvePackTypeRef()?.coneType
            ?: return when (this) {
                PackSelectorKind.Props,
                PackSelectorKind.SharedProps -> true
                PackSelectorKind.Slots -> typeRef.hasPackComposableAnnotationBySource()
                else -> false
            }
        if (this == PackSelectorKind.Slots && typeRef.hasPackComposableAnnotationBySource()) {
            return coneType.isSomeFunctionType(session)
        }
        return matchesPackProjection(coneType)
    }

    private fun PackSelectorKind.matchesPackProjection(type: ConeKotlinType): Boolean {
        return when (this) {
            PackSelectorKind.Props,
            PackSelectorKind.SharedProps -> true
            PackSelectorKind.Attrs -> !type.isSomeFunctionType(session)
            PackSelectorKind.Callbacks -> type.isSomeFunctionType(session) && !type.isPackComposableFunctionType()
            PackSelectorKind.Slots -> type.isPackComposableFunctionType()
        }
    }

    private fun ConeKotlinType.isPackComposableFunctionType(): Boolean {
        if (!isSomeFunctionType(session)) {
            return false
        }

        val kind = functionTypeKind(session)
        if (kind != null && !kind.isBasicFunctionOrKFunction && kind != FunctionTypeKind.SuspendFunction && kind != FunctionTypeKind.KSuspendFunction) {
            return true
        }

        return customAnnotations.any { annotation ->
            val classId = annotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId
            classId?.shortClassName?.asString() == "Composable" || annotation.hasPackComposableAnnotationBySource()
        }
    }

    private fun FirAnnotation.hasPackComposableAnnotationBySource(): Boolean {
        return source?.text?.toString().hasPackComposableAnnotationBySource() ||
                annotationTypeRef.source?.text?.toString().hasPackComposableAnnotationBySource()
    }

    private fun PackSelectorKind.matchesPackProjection(type: ConeKotlinType, sourceTypeText: String?): Boolean {
        if (this == PackSelectorKind.Slots && sourceTypeText.hasPackComposableAnnotationBySource()) {
            return type.isSomeFunctionType(session)
        }

        return matchesPackProjection(type)
    }

    private fun String?.hasPackComposableAnnotationBySource(): Boolean {
        val sourceText = this ?: return false
        val normalizedText = sourceText.filterNot(Char::isWhitespace)
        return PACK_COMPOSABLE_ANNOTATION_REGEX.containsMatchIn(normalizedText)
    }

    private fun FirTypeRef.hasPackComposableAnnotationBySource(): Boolean {
        return source?.text?.toString().hasPackComposableAnnotationBySource()
    }

    private data class PackSelector(
        val receiverName: Name,
        val kind: PackSelectorKind,
        val overloadSelectorName: Name? = null,
    ) {
        fun render(): String = buildString {
            append(receiverName.asString())
            append('.')
            append(kind.selectorText)
            if (overloadSelectorName != null) {
                append('(')
                append(overloadSelectorName.asString())
                append(')')
            }
        }
    }

    private enum class PackSelectorKind {
        Props,
        SharedProps,
        Attrs,
        Callbacks,
        Slots;

        val selectorText: String
            get() = when (this) {
                Props -> "\$props"
                SharedProps -> "\$sharedProps"
                Attrs -> "\$attrs"
                Callbacks -> "\$callbacks"
                Slots -> "\$slots"
            }

        val supportsOverloadSelector: Boolean
            get() = this != SharedProps
    }

    private data class PackFunctionShape(
        val parameterNames: List<Name>,
        val parameterTypes: List<ConeKotlinType>,
        val parameterSourceTexts: List<String>? = null,
    )

    private data class PackFunctionTypeParameterSource(
        val name: Name,
        val typeText: String,
    )

    private companion object {
        val PACK_COMPOSABLE_ANNOTATION_REGEX = Regex("@(?:[A-Za-z_][A-Za-z0-9_]*\\.)*Composable(?:\\(|\\b)")
    }

    private fun unboundCyclesInTypeParametersSupertypes(typeParametersOwner: FirTypeParameterRefsOwner) {
        for (typeParameter in typeParametersOwner.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            if (hasSupertypePathToParameter(typeParameter, typeParameter, mutableSetOf())) {
                val errorType = buildErrorTypeRef {
                    diagnostic = ConeCyclicTypeBound(typeParameter.symbol, typeParameter.bounds.toImmutableList())
                    source = typeParameter.bounds.first().source
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

        fun ConeKotlinType.toNextTypeParameter(): FirTypeParameter? = when (this) {
            is ConeTypeParameterType -> lookupTag.typeParameterSymbol.fir
            is ConeDefinitelyNotNullType -> original.toNextTypeParameter()
            else -> null
        }

        return currentTypeParameter.bounds.any {
            val nextTypeParameter = it.coneTypeOrNull?.toNextTypeParameter() ?: return@any false

            hasSupertypePathToParameter(nextTypeParameter, typeParameter, visited)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): FirTypeRef {
        return implicitTypeRef
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): FirResolvedTypeRef {
        return typeRef.transform(
            typeResolverTransformer,
            TypeResolutionConfiguration(scopes.asReversed(), classDeclarationsStack, currentFile)
        )
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: Any?,
    ): FirStatement = whileAnalysing(session, valueParameter) {
        withDeclaration(valueParameter) {
            valueParameter.transformReturnTypeRef(this, data)
            valueParameter.transformAnnotations(this, data)
            valueParameter.transformVarargTypeToArrayType(session)
            valueParameter
        }
    }

    override fun transformBlock(block: FirBlock, data: Any?): FirStatement {
        return block
    }

    override fun transformArgumentList(argumentList: FirArgumentList, data: Any?): FirArgumentList {
        return argumentList
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
                        annotationCall.transformTypeArguments(this, data)
                        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types)
                        val alternativeResolvedTypeRef =
                            originalTypeRef.delegatedTypeRef?.transformSingle(this, data) ?: return annotationCall
                        val coneTypeFromCompilerRequiredPhase = originalTypeRef.coneType
                        val coneTypeFromTypesPhase = alternativeResolvedTypeRef.coneType
                        if (coneTypeFromTypesPhase != coneTypeFromCompilerRequiredPhase) {
                            val errorTypeRef = buildErrorTypeRef {
                                source = originalTypeRef.source
                                coneType = coneTypeFromCompilerRequiredPhase
                                annotations += originalTypeRef.annotations
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
                annotationCall.transformTypeArguments(this, data)
                annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types)
                annotationCall.replaceAnnotationTypeRef(transformedTypeRef)
            }
        }

        return annotationCall
    }

    inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val scopesBeforeSnapshot = scopes
        val scopesBeforeBeforeSnapshot = scopesBefore
        scopesBefore = scopesBeforeSnapshot

        val staticScopesBeforeSnapshot = staticScopes
        val staticScopesBeforeBeforeSnapshot = staticScopesBefore
        staticScopesBefore = staticScopesBeforeSnapshot

        return try {
            l()
        } finally {
            scopes = scopesBeforeSnapshot
            scopesBefore = scopesBeforeBeforeSnapshot
            staticScopes = staticScopesBeforeSnapshot
            staticScopesBefore = staticScopesBeforeBeforeSnapshot
        }
    }

    private fun resolveClassContent(
        firClass: FirClass,
        data: Any?
    ): FirStatement = withClassScopes(
        firClass,
        actionInsideStaticScope = {
            withScopeCleanup {
                firClass.transformAnnotations(this, null)

                if (firClass is FirRegularClass) {
                    addTypeParametersScope(firClass)
                }

                // ConstructedTypeRef should be resolved only with type parameters, but not with nested classes and classes from supertypes
                for (declaration in firClass.declarations) {
                    when (declaration) {
                        is FirConstructor -> transformDelegatedConstructorCall(declaration)
                        is FirField -> {
                            if (declaration.origin == FirDeclarationOrigin.Synthetic.DelegateField) {
                                transformDelegateField(declaration)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    ) {
        // Note that annotations are still visited here
        // again, although there's no need in it
        transformElement(firClass, data)
    }

    fun transformDelegatedConstructorCall(constructor: FirConstructor) {
        constructor.delegatedConstructor?.let(this::resolveConstructedTypeRefForDelegatedConstructorCall)
    }

    fun transformDelegateField(field: FirField) {
        field.transformReturnTypeRef(this, null)
    }

    fun removeOuterTypeParameterScope(firClass: FirClass): Boolean = !firClass.isInner && !firClass.isLocal

    /**
     * Changes to the order of scopes should also be reflected in
     * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext.withScopesForClass].
     * Otherwise, we get different behavior between type resolve and body resolve phases.
     */
    inline fun <R> withClassScopes(
        firClass: FirClass,
        crossinline actionInsideStaticScope: () -> Unit = {},
        crossinline action: () -> R,
    ): R = withScopeCleanup {
        // Remove type parameter scopes for classes that are neither inner nor local
        if (removeOuterTypeParameterScope(firClass)) {
            this.scopes = staticScopes
        }

        actionInsideStaticScope()

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

        if (firClass is FirRegularClass) {
            // Companion scope is added before static scope,
            // i.e., static scope is checked first during resolution (scopes are in reverse order).
            // This is because we can qualify companion scope using `Companion.` if we want to explicitly refer to a declaration in the
            // companion.
            firClass.companionObjectSymbol?.fir
                ?.let(session::nestedClassifierScope)
                ?.let(scopesToAdd::add)

            session.nestedClassifierScope(firClass)?.let(scopesToAdd::add)

            addScopes(scopesToAdd)
            addTypeParametersScope(firClass)
        } else {
            session.nestedClassifierScope(firClass)?.let(scopesToAdd::add)
            addScopes(scopesToAdd)
        }

        action()
    }

    private fun resolveConstructedTypeRefForDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall
    ) {
        delegatedConstructorCall.replaceConstructedTypeRef(delegatedConstructorCall.constructedTypeRef.transformSingle(this, null))
        delegatedConstructorCall.transformCalleeReference(this, null)
    }

    fun addTypeParametersScope(firMemberDeclaration: FirMemberDeclaration) {
        if (firMemberDeclaration.typeParameters.isNotEmpty()) {
            scopes = scopes.add(FirMemberTypeParameterScope(firMemberDeclaration))
        }
    }

    fun addScopes(list: List<FirScope>) {
        // small optimization to skip unnecessary allocations
        val scopesAreTheSame = scopes === staticScopes

        scopes = scopes.addAll(list)
        staticScopes = if (scopesAreTheSame) scopes else staticScopes.addAll(list)
    }

    /**
     * Filters annotations by target.
     * For example, in the following snippet the annotation may apply to the constructor value parameter, the property or the underlying field:
     * ```
     * class Foo(@Ann val x: String)
     * ```
     * This ambiguity may be resolved by specifying the use-site explicitly, i.e. `@field:Ann` or by analysing the allowed targets from
     * the [kotlin.annotation.Target] meta-annotation.
     * In latter case, the method will ensure that the annotation is moved to the correct element (field or parameter) or left at the property.
     */
    private fun FirVariable.moveOrDeleteIrrelevantAnnotations() {
        if (annotations.isEmpty()) return
        val languageVersionSettings = session.languageVersionSettings
        replaceAnnotations(annotations.filter { annotation ->
            when (annotation.useSiteTarget) {
                null -> annotation.multiplexWithoutUseSiteTarget(this, languageVersionSettings)
                ALL -> annotation.multiplexWithAllUseSiteTarget(this, languageVersionSettings)
                else -> true
            }
        })
    }

    private fun FirAnnotation.multiplexWithoutUseSiteTarget(
        annotated: FirDeclaration,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        val allowedTargets = useSiteTargetsFromMetaAnnotation(session)
        return when (annotated) {
            // If parameter is allowed, we apply annotation to it in the first turn, independent of the targeting mode
            is FirValueParameter -> {
                CONSTRUCTOR_PARAMETER in allowedTargets
            }
            is FirProperty if annotated.fromPrimaryConstructor == true && CONSTRUCTOR_PARAMETER in allowedTargets -> {
                when {
                    !languageVersionSettings.supportsFeature(LanguageFeature.PropertyParamAnnotationDefaultTargetMode) -> {
                        false
                    }
                    // In the property-param mode,
                    // we should apply annotation also to the property (or to the field) if it's allowed
                    PROPERTY in allowedTargets -> true
                    annotated.backingField != null && propertyAnnotationShouldBeMovedToField(allowedTargets) -> {
                        if (classDeclarationsStack.lastOrNull()?.classKind != ClassKind.ANNOTATION_CLASS) {
                            val backingField = annotated.backingField!!
                            backingField.replaceAnnotations(backingField.annotations + this)
                        }
                        false
                    }
                    else -> false
                }
            }
            // Otherwise (for a regular property or for a constructor property if annotation isn't applicable to parameter),
            // we simply choose between a property and a field
            is FirProperty if annotated.backingField != null && propertyAnnotationShouldBeMovedToField(allowedTargets) -> {
                val backingField = annotated.backingField!!
                backingField.replaceAnnotations(backingField.annotations + this)
                false
            }
            // Here we can come with a regular (non-constructor) property without a backing field,
            // or with some other non-parameter variable
            else -> {
                true
            }
        }
    }

    private fun FirAnnotation.multiplexWithAllUseSiteTarget(
        annotated: FirDeclaration,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.AnnotationAllUseSiteTarget)) {
            return true
        }
        val allowedTargets = useSiteTargetsFromMetaAnnotation(session)
        return when (annotated) {
            is FirValueParameter -> {
                CONSTRUCTOR_PARAMETER in allowedTargets
            }
            is FirProperty -> {
                var addedSomewhere = false

                fun FirCallableDeclaration.addAnnotationWithoutUseSiteTarget(annotation: FirAnnotation) {
                    val copy = if (annotation is FirAnnotationCall) {
                        buildAnnotationCallCopy(annotation) {
                            useSiteTarget = null
                        }
                    } else {
                        buildAnnotationCopy(annotation) {
                            useSiteTarget = null
                        }
                    }
                    replaceAnnotations(annotations + copy)
                    addedSomewhere = true
                }

                if (FIELD in allowedTargets && annotated.delegate == null) {
                    annotated.backingField?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (PROPERTY_GETTER in allowedTargets) {
                    annotated.getter?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (annotated.isVar && SETTER_PARAMETER in allowedTargets) {
                    annotated.setter?.valueParameters?.firstOrNull()?.addAnnotationWithoutUseSiteTarget(this)
                }
                if (CONSTRUCTOR_PARAMETER in allowedTargets && annotated.fromPrimaryConstructor == true) {
                    // It's already on a constructor parameter, but we set the flag to prevent reporting an error
                    addedSomewhere = true
                }
                // If annotation isn't applicable anywhere or the property is delegated, we keep it at property to report an error later
                PROPERTY in allowedTargets || !addedSomewhere || annotated.delegate != null
            }
            else -> {
                true
            }
        }
    }

    /**
     * @param allowedTargets allowed use-site targets of a given property annotation
     * @return true if the given annotation on a property (initially placed there during raw FIR building)
     * is in fact inapplicable to properties, but applicable to fields.
     */
    private fun propertyAnnotationShouldBeMovedToField(allowedTargets: Set<AnnotationUseSiteTarget>): Boolean =
        (FIELD in allowedTargets || PROPERTY_DELEGATE_FIELD in allowedTargets) && PROPERTY !in allowedTargets
}
