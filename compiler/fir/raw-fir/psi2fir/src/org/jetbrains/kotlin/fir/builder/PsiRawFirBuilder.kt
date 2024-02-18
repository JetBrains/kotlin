/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.AstLoadingFilter
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.isCallTheFirstStatement
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

open class PsiRawFirBuilder(
    session: FirSession,
    val baseScopeProvider: FirScopeProvider,
    bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawFirBuilder<PsiElement>(session) {
    protected open fun bindFunctionTarget(target: FirFunctionTarget, function: FirFunction) {
        target.bind(function)
    }

    var mode: BodyBuildingMode = bodyBuildingMode
        private set

    private inline fun <T> runOnStubs(crossinline body: () -> T): T {
        return when (mode) {
            BodyBuildingMode.NORMAL -> body()
            BodyBuildingMode.LAZY_BODIES -> {
                AstLoadingFilter.disallowTreeLoading<T, Nothing> { body() }
            }
        }
    }

    fun buildFirFile(file: KtFile): FirFile {
        return runOnStubs { file.accept(Visitor(), null) as FirFile }
    }

    fun buildAnnotationCall(annotation: KtAnnotationEntry, containerSymbol: FirBasedSymbol<*>): FirAnnotationCall {
        return withContainerSymbol(containerSymbol) {
            Visitor().visitAnnotationEntry(annotation, null) as FirAnnotationCall
        }
    }

    override fun PsiElement.toFirSourceElement(kind: KtFakeSourceElementKind?): KtPsiSourceElement {
        val actualKind = kind ?: KtRealSourceElementKind
        return this.toKtPsiSourceElement(actualKind)
    }

    override val PsiElement.elementType: IElementType
        get() = node.elementType

    override val PsiElement.asText: String
        get() = text

    override val PsiElement.unescapedValue: String
        get() = (this as KtEscapeStringTemplateEntry).unescapedValue

    override fun PsiElement.getChildNodeByType(type: IElementType): PsiElement? {
        return children.firstOrNull { it.node.elementType == type }
    }

    override fun PsiElement.getReferencedNameAsName(): Name {
        return (this as KtSimpleNameExpression).getReferencedNameAsName()
    }

    override fun PsiElement.getLabelName(): String? {
        return when (this) {
            is KtExpressionWithLabel -> getLabelName()
            is KtNamedFunction -> parent.getLabelName()
            else -> null
        }
    }

    override fun PsiElement.getExpressionInParentheses(): PsiElement? {
        return (this as KtParenthesizedExpression).expression
    }

    override fun PsiElement.getAnnotatedExpression(): PsiElement? {
        return (this as KtAnnotatedExpression).baseExpression
    }

    override fun PsiElement.getLabeledExpression(): PsiElement? {
        return (this as KtLabeledExpression).baseExpression
    }

    override val PsiElement?.receiverExpression: PsiElement?
        get() = (this as? KtQualifiedExpression)?.receiverExpression

    override val PsiElement?.selectorExpression: PsiElement?
        get() = (this as? KtQualifiedExpression)?.selectorExpression

    override val PsiElement?.arrayExpression: PsiElement?
        get() = (this as? KtArrayAccessExpression)?.arrayExpression

    override val PsiElement?.indexExpressions: List<PsiElement>?
        get() = (this as? KtArrayAccessExpression)?.indexExpressions

    override val PsiElement.isVararg: Boolean
        get() = (this as? KtParameter)?.isVarArg == true

    private fun KtModifierListOwner.getVisibility(publicByDefault: Boolean = false): Visibility =
        with(modifierList) {
            when {
                this == null -> null
                hasModifier(PRIVATE_KEYWORD) -> Visibilities.Private
                hasModifier(PUBLIC_KEYWORD) -> Visibilities.Public
                hasModifier(PROTECTED_KEYWORD) -> Visibilities.Protected
                hasModifier(INTERNAL_KEYWORD) -> Visibilities.Internal
                else -> null
            } ?: if (publicByDefault) Visibilities.Public else Visibilities.Unknown
        }

    private val KtDeclaration.modality: Modality?
        get() = with(modifierList) {
            when {
                this == null -> null
                hasModifier(FINAL_KEYWORD) -> Modality.FINAL
                hasModifier(SEALED_KEYWORD) -> if (this@modality is KtClassOrObject) Modality.SEALED else null
                hasModifier(ABSTRACT_KEYWORD) -> Modality.ABSTRACT
                else -> if (hasModifier(OPEN_KEYWORD)) Modality.OPEN else null
            }
        }

    protected open inner class Visitor : KtVisitor<FirElement, FirElement?>(), DestructuringContext<KtDestructuringDeclarationEntry> {

        override val KtDestructuringDeclarationEntry.returnTypeRef: FirTypeRef
            get() = typeReference.toFirOrImplicitType()

        @Suppress("ConflictingExtensionProperty")
        override val KtDestructuringDeclarationEntry.name: Name
            get() = if (nameIdentifier?.text == "_") {
                SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            } else {
                nameAsSafeName
            }

        override val KtDestructuringDeclarationEntry.source: KtSourceElement
            get() = toKtPsiSourceElement()

        override fun KtDestructuringDeclarationEntry.extractAnnotationsTo(
            target: FirAnnotationContainerBuilder,
            containerSymbol: FirBasedSymbol<*>,
        ) {
            (this as KtAnnotated).extractAnnotationsTo(target)
        }

        override fun createComponentCall(
            container: FirVariable,
            entrySource: KtSourceElement?,
            index: Int,
        ): FirExpression = buildOrLazyExpression(entrySource) {
            super.createComponentCall(container, entrySource, index)
        }

        private inline fun <reified R : FirElement> KtElement?.convertSafe(): R? =
            this?.let { convertElement(it, null) } as? R

        private inline fun <reified R : FirElement> KtElement.convert(): R =
            convertElement(this, null) as R

        private inline fun <T> buildOrLazy(build: () -> T, noinline lazy: () -> T): T {
            return when (mode) {
                BodyBuildingMode.NORMAL -> build()
                BodyBuildingMode.LAZY_BODIES -> runOnStubs(lazy)
            }
        }

        private inline fun buildOrLazyExpression(
            sourceElement: KtSourceElement?,
            buildExpression: () -> FirExpression,
        ): FirExpression {
            return buildOrLazy(buildExpression) {
                buildLazyExpression {
                    source = sourceElement
                }
            }
        }

        private inline fun buildOrLazyBlock(buildBlock: () -> FirBlock): FirBlock {
            return buildOrLazy(buildBlock, ::buildLazyBlock)
        }

        private inline fun buildOrLazyDelegatedConstructorCall(
            isThis: Boolean,
            constructedTypeRef: FirTypeRef,
            buildCall: () -> FirDelegatedConstructorCall,
        ): FirDelegatedConstructorCall {
            return buildOrLazy(buildCall) {
                buildLazyDelegatedConstructorCall {
                    this.isThis = isThis
                    this.constructedTypeRef = constructedTypeRef
                    calleeReference = if (isThis) {
                        buildExplicitThisReference {
                            source = null
                        }
                    } else {
                        buildExplicitSuperReference {
                            source = null
                            superTypeRef = constructedTypeRef
                        }
                    }
                }
            }
        }

        fun convertElement(element: KtElement, original: FirElement? = null): FirElement? =
            element.accept(this@Visitor, original)

        fun convertProperty(
            property: KtProperty,
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
        ): FirProperty = property.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol,
            context
        )

        private fun KtTypeReference?.toFirOrImplicitType(): FirTypeRef =
            this?.toFirType() ?: FirImplicitTypeRefImplWithoutSource

        private fun KtTypeReference?.toFirOrUnitType(): FirTypeRef =
            this?.toFirType() ?: implicitUnitType

        protected fun KtTypeReference?.toFirOrErrorType(): FirTypeRef =
            this?.toFirType() ?: buildErrorTypeRef {
                source = this@toFirOrErrorType?.toFirSourceElement()
                diagnostic = ConeSyntaxDiagnostic(
                    if (this@toFirOrErrorType == null) "Incomplete code" else "Conversion failed"
                )
                this@toFirOrErrorType?.extractAnnotationsTo(this)
            }

        // Here we accept lambda as receiver to prevent expression calculation in stub mode
        private fun (() -> KtExpression?).toFirExpression(errorReason: String): FirExpression =
            this().toFirExpression(errorReason)

        private fun KtElement?.toFirExpression(
            errorReason: String,
            kind: DiagnosticKind = DiagnosticKind.ExpressionExpected,
        ): FirExpression = toFirExpression { ConeSimpleDiagnostic(errorReason, kind) }

        private inline fun KtElement?.toFirExpression(
            sourceWhenThisIsNull: KtElement? = null,
            sourceWhenInvalidExpression: KtElement? = this,
            isValidExpression: (FirExpression) -> Boolean = { !it.isStatementLikeExpression },
            diagnosticFn: () -> ConeDiagnostic,
        ): FirExpression {
            if (this == null) {
                return buildErrorExpression(source = sourceWhenThisIsNull?.toFirSourceElement(), diagnosticFn())
            }

            return when (val fir = convertElement(this, null)) {
                is FirExpression -> when {
                    isValidExpression(fir) -> checkSelectorInvariant(fir)
                    else -> buildErrorExpression {
                        nonExpressionElement = fir
                        diagnostic = diagnosticFn()
                        source = sourceWhenInvalidExpression?.toFirSourceElement()
                    }
                }
                else -> buildErrorExpression {
                    nonExpressionElement = fir
                    diagnostic = diagnosticFn()
                    source = fir?.source?.realElement() ?: toFirSourceElement()
                }
            }
        }

        private fun KtElement.checkSelectorInvariant(result: FirExpression): FirExpression {
            val callExpressionCallee = (this as? KtCallExpression)?.calleeExpression?.unwrapParenthesesLabelsAndAnnotations()

            if (this is KtNameReferenceExpression ||
                (this is KtCallExpression && callExpressionCallee !is KtLambdaExpression) ||
                getQualifiedExpressionForSelector() == null
            ) {
                return result
            }

            return buildErrorExpression {
                source = callExpressionCallee?.toFirSourceElement() ?: toFirSourceElement()
                diagnostic =
                    ConeSimpleDiagnostic(
                        "The expression cannot be a selector (occur after a dot)",
                        if (callExpressionCallee == null) DiagnosticKind.IllegalSelector else DiagnosticKind.NoReceiverAllowed
                    )
                expression = result
            }
        }

        private inline fun KtExpression.toFirStatement(errorReasonLazy: () -> String): FirStatement {
            return when (val fir = convertElement(this, null)) {
                is FirStatement -> fir
                else -> buildErrorExpression {
                    nonExpressionElement = fir
                    diagnostic = ConeSyntaxDiagnostic(errorReasonLazy())
                    source = toFirSourceElement()
                }
            }
        }

        private fun KtExpression.toFirStatement(): FirStatement =
            convert()

        private fun KtDeclaration.toFirDeclaration(
            delegatedSuperType: FirTypeRef,
            delegatedSelfType: FirResolvedTypeRef,
            owner: KtClassOrObject,
            ownerClassBuilder: FirClassBuilder,
            ownerTypeParameters: List<FirTypeParameterRef>,
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> {
                    toFirConstructor(
                        if (isDelegatedCallToThis()) delegatedSelfType else delegatedSuperType,
                        delegatedSelfType,
                        owner,
                        ownerTypeParameters,
                    )
                }
                is KtEnumEntry -> {
                    val primaryConstructor = owner.primaryConstructor
                    val ownerClassHasDefaultConstructor =
                        primaryConstructor?.valueParameters?.isEmpty() ?: owner.secondaryConstructors.let { constructors ->
                            constructors.isEmpty() || constructors.any { it.valueParameters.isEmpty() }
                        }
                    toFirEnumEntry(delegatedSelfType, ownerClassHasDefaultConstructor)
                }
                is KtProperty -> {
                    convertProperty(
                        this@toFirDeclaration,
                        ownerClassBuilder.ownerRegularOrAnonymousObjectSymbol
                    )
                }
                is KtDestructuringDeclaration -> {
                    buildErrorTopLevelDestructuringDeclaration(toFirSourceElement())
                }
                is KtClassInitializer -> {
                    buildAnonymousInitializer(this, ownerClassBuilder.ownerRegularOrAnonymousObjectSymbol)
                }
                else -> convert()
            }
        }

        private fun KtExpression?.toFirBlock(): FirBlock =
            when (this) {
                is KtBlockExpression ->
                    accept(this@Visitor, null) as FirBlock
                null ->
                    buildEmptyExpressionBlock()
                else -> {
                    var firBlock: FirBlock? = null
                    if (this is KtAnnotatedExpression) {
                        val lastChild = children.lastOrNull()
                        if (lastChild is KtBlockExpression) {
                            firBlock = lastChild.toFirBlock()
                            extractAnnotationsTo(firBlock)
                        }
                    }
                    firBlock ?: FirSingleExpressionBlock(convert())
                }
            }

        private fun KtDeclarationWithBody.buildFirBody(): Pair<FirBlock?, FirContractDescription?> =
            if (hasBody()) {
                buildOrLazyBlock {
                    if (hasBlockBody()) {
                        val block = bodyBlockExpression?.accept(this@Visitor, null) as? FirBlock
                        val contractDescription = when {
                            !hasContractEffectList() -> block?.let {
                                val blockSourcePsi = it.source?.psi
                                val diagnostic = when {
                                    blockSourcePsi == null || !isCallTheFirstStatement(blockSourcePsi) -> ConeContractShouldBeFirstStatement
                                    else -> null
                                }
                                processLegacyContractDescription(block, diagnostic)
                            }
                            else -> null
                        }
                        return@buildFirBody block to contractDescription
                    } else {
                        val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                        FirSingleExpressionBlock(result.toReturn(baseSource = result.source))
                    }
                } to null
            } else {
                null to null
            }

        private fun isCallTheFirstStatement(psi: PsiElement): Boolean =
            isCallTheFirstStatement(psi, { it.elementType }, { it.allChildren.toList() })

        private fun ValueArgument?.toFirExpression(): FirExpression {
            if (this == null) {
                return buildErrorExpression(
                    source = null,
                    ConeSyntaxDiagnostic("No argument given"),
                )
            }
            val name = this.getArgumentName()?.asName
            val firExpression = when (val expression = this.getArgumentExpression()) {
                is KtConstantExpression, is KtStringTemplateExpression -> {
                    expression.accept(this@Visitor, null) as FirExpression
                }

                else -> {
                    { expression }.toFirExpression("Argument is absent")
                }
            }
            val isSpread = getSpreadElement() != null
            return when {
                name != null -> buildNamedArgumentExpression {
                    source = (this@toFirExpression as? PsiElement)?.toFirSourceElement()
                    this.expression = firExpression
                    this.isSpread = isSpread
                    this.name = name
                }
                isSpread -> buildSpreadArgumentExpression {
                    source = (this@toFirExpression as? PsiElement)?.toFirSourceElement()
                    this.expression = firExpression
                }
                else -> firExpression
            }
        }

        private fun KtPropertyAccessor?.toFirPropertyAccessor(
            property: KtProperty,
            propertyTypeRef: FirTypeRef,
            propertySymbol: FirPropertySymbol,
            isGetter: Boolean,
            accessorAnnotationsFromProperty: List<FirAnnotation>,
            parameterAnnotationsFromProperty: List<FirAnnotation>,
        ): FirPropertyAccessor? {
            val defaultVisibility = this?.getVisibility()
            val accessorVisibility =
                if (defaultVisibility != null && defaultVisibility != Visibilities.Unknown) defaultVisibility else property.getVisibility()
            // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
            val status =
                FirDeclarationStatusImpl(accessorVisibility, this?.modality).apply {
                    isInline = property.hasModifier(INLINE_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(INLINE_KEYWORD) == true
                    isExternal = property.hasModifier(EXTERNAL_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(EXTERNAL_KEYWORD) == true
                }
            val propertyTypeRefToUse = propertyTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
            return when {
                this != null && hasBody() -> {
                    // Property has a non-default getter or setter.
                    // NOTE: We still need the setter even for a val property so we can report errors (e.g., VAL_WITH_SETTER).
                    val source = this.toFirSourceElement()
                    val accessorTarget = FirFunctionTarget(labelName = null, isLambda = false)
                    buildPropertyAccessor {
                        this.source = source
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = if (isGetter) {
                            returnTypeReference?.toFirType() ?: propertyTypeRefToUse
                        } else {
                            returnTypeReference.toFirOrUnitType()
                        }
                        this.isGetter = isGetter
                        this.status = status
                        annotations += accessorAnnotationsFromProperty
                        extractAnnotationsTo(this)
                        this@PsiRawFirBuilder.context.firFunctionTargets += accessorTarget
                        symbol = FirPropertyAccessorSymbol()
                        extractValueParametersTo(
                            this, symbol, ValueParameterDeclaration.SETTER, propertyTypeRefToUse, parameterAnnotationsFromProperty
                        )
                        if (!isGetter && valueParameters.isEmpty()) {
                            valueParameters += buildDefaultSetterValueParameter {
                                this.source = source.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                returnTypeRef = propertyTypeRefToUse
                                symbol = FirValueParameterSymbol(StandardNames.DEFAULT_VALUE_PARAMETER)
                                annotations += parameterAnnotationsFromProperty
                            }
                        }
                        val outerContractDescription = this@toFirPropertyAccessor.obtainContractDescription()
                        val (body, innerContractDescription) = withForcedLocalContext {
                            this@toFirPropertyAccessor.buildFirBody()
                        }
                        this.body = body
                        val contractDescription = outerContractDescription ?: innerContractDescription
                        contractDescription?.let {
                            this.contractDescription = it
                        }
                        this.propertySymbol = propertySymbol
                    }.also {
                        it.initContainingClassAttr()
                        bindFunctionTarget(accessorTarget, it)
                        this@PsiRawFirBuilder.context.firFunctionTargets.removeLast()
                    }
                }
                isGetter || property.isVar -> {
                    // Default getter for val/var properties, and default setter for var properties.
                    val propertySource =
                        this?.toFirSourceElement() ?: property.toKtPsiSourceElement(KtFakeSourceElementKind.DefaultAccessor)
                    FirDefaultPropertyAccessor
                        .createGetterOrSetter(
                            propertySource,
                            baseModuleData,
                            FirDeclarationOrigin.Source,
                            propertyTypeRefToUse,
                            accessorVisibility,
                            propertySymbol,
                            isGetter,
                            parameterAnnotations = parameterAnnotationsFromProperty
                        )
                        .also {
                            if (this != null) {
                                it.extractAnnotationsFrom(this)
                            }
                            it.replaceAnnotations(it.annotations.smartPlus(accessorAnnotationsFromProperty))
                            it.status = status
                            it.initContainingClassAttr()
                        }
                }
                else -> {
                    // No default setter for val properties.
                    null
                }
            }
        }

        private fun obtainPropertyComponentStatus(
            componentVisibility: Visibility,
            declaration: KtDeclaration?,
            property: KtProperty,
        ): FirDeclarationStatus {
            return FirDeclarationStatusImpl(componentVisibility, declaration?.modality).apply {
                isInline = property.hasModifier(INLINE_KEYWORD) ||
                        declaration?.hasModifier(INLINE_KEYWORD) == true
                isExternal = property.hasModifier(EXTERNAL_KEYWORD) ||
                        declaration?.hasModifier(EXTERNAL_KEYWORD) == true
                isLateInit = declaration?.hasModifier(LATEINIT_KEYWORD) == true
            }
        }

        private fun KtBackingField?.toFirBackingField(
            property: KtProperty,
            propertySymbol: FirPropertySymbol,
            propertyReturnType: FirTypeRef,
            annotationsFromProperty: List<FirAnnotationCall>,
        ): FirBackingField {
            val defaultVisibility = this?.getVisibility()
            val componentVisibility = if (defaultVisibility != null && defaultVisibility != Visibilities.Unknown) {
                defaultVisibility
            } else {
                Visibilities.Private
            }
            val status = obtainPropertyComponentStatus(componentVisibility, this, property)
            val backingFieldInitializer = this?.toInitializerExpression()
            val returnType = this?.returnTypeReference.toFirOrImplicitType()
            val source = this?.toFirSourceElement()
            return if (this != null) {
                buildBackingField {
                    this.source = source
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = returnType
                    this.status = status
                    extractAnnotationsTo(this)
                    this.annotations += annotationsFromProperty
                    name = BACKING_FIELD
                    symbol = FirBackingFieldSymbol(CallableId(name))
                    this.propertySymbol = propertySymbol
                    this.initializer = backingFieldInitializer
                    this.isVar = property.isVar
                    this.isVal = !property.isVar
                }
            } else {
                FirDefaultPropertyBackingField(
                    moduleData = baseModuleData,
                    origin = FirDeclarationOrigin.Source,
                    source = property.toFirSourceElement(KtFakeSourceElementKind.DefaultAccessor),
                    annotations = annotationsFromProperty.toMutableList(),
                    returnTypeRef = propertyReturnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                    isVar = property.isVar,
                    propertySymbol = propertySymbol,
                    status = status,
                )
            }
        }

        private fun KtParameter.toFirValueParameter(
            defaultTypeRef: FirTypeRef?,
            functionSymbol: FirFunctionSymbol<*>,
            valueParameterDeclaration: ValueParameterDeclaration,
            additionalAnnotations: List<FirAnnotation> = emptyList(),
        ): FirValueParameter {
            val name = convertValueParameterName(nameAsSafeName, valueParameterDeclaration) { nameIdentifier?.node?.text }
            return buildValueParameter {
                val parameterSource = toFirSourceElement()
                source = parameterSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                isVararg = isVarArg
                this.name = name
                symbol = FirValueParameterSymbol(name)
                withContainerSymbol(symbol, isLocal = valueParameterDeclaration != ValueParameterDeclaration.FUNCTION) {
                    returnTypeRef = when {
                        typeReference != null -> typeReference.toFirOrErrorType()
                        defaultTypeRef != null -> defaultTypeRef
                        valueParameterDeclaration.shouldExplicitParameterTypeBePresent -> createNoTypeForParameterTypeRef(parameterSource)
                        else -> null.toFirOrImplicitType()
                    }.let {
                        if (isVararg && it is FirErrorTypeRef) {
                            it.wrapIntoArray()
                        } else {
                            it
                        }
                    }

                    addAnnotationsFrom(
                        this@toFirValueParameter,
                        isFromPrimaryConstructor = valueParameterDeclaration == ValueParameterDeclaration.PRIMARY_CONSTRUCTOR
                    )
                }

                defaultValue = if (hasDefaultValue()) {
                    buildOrLazyExpression(null) { { this@toFirValueParameter.defaultValue }.toFirExpression("Should have default value") }
                } else null
                isCrossinline = hasModifier(CROSSINLINE_KEYWORD)
                isNoinline = hasModifier(NOINLINE_KEYWORD)
                containingFunctionSymbol = functionSymbol
                annotations += additionalAnnotations
            }
        }

        private fun FirValueParameterBuilder.addAnnotationsFrom(ktParameter: KtParameter, isFromPrimaryConstructor: Boolean) {
            for (annotationEntry in ktParameter.annotationEntries) {
                annotationEntry.convert<FirAnnotation>().takeIf {
                    !isFromPrimaryConstructor || it.useSiteTarget.appliesToPrimaryConstructorParameter()
                }?.let {
                    annotations += it
                }
            }
        }

        private fun KtParameter.toFirProperty(firParameter: FirValueParameter): FirProperty {
            require(hasValOrVar())
            val status = FirDeclarationStatusImpl(getVisibility(), modality).apply {
                isExpect = hasExpectModifier() || this@PsiRawFirBuilder.context.containerIsExpect
                isActual = hasActualModifier()
                isOverride = hasModifier(OVERRIDE_KEYWORD)
                isConst = hasModifier(CONST_KEYWORD)
                isLateInit = hasModifier(LATEINIT_KEYWORD)
            }

            val propertyName = nameAsSafeName
            val propertySymbol = FirPropertySymbol(callableIdForName(propertyName))
            withContainerSymbol(propertySymbol) {
                val propertySource = toFirSourceElement(KtFakeSourceElementKind.PropertyFromParameter)
                val parameterAnnotations = mutableListOf<FirAnnotationCall>()
                for (annotationEntry in annotationEntries) {
                    parameterAnnotations += annotationEntry.convert<FirAnnotationCall>()
                }

                return buildProperty {
                    source = propertySource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = firParameter.returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyFromParameter)
                    name = propertyName
                    initializer = buildPropertyAccessExpression {
                        source = propertySource
                        calleeReference = buildPropertyFromParameterResolvedNamedReference {
                            source = propertySource
                            name = propertyName
                            resolvedSymbol = firParameter.symbol
                        }
                    }
                    isVar = isMutable
                    symbol = propertySymbol
                    isLocal = false
                    val defaultAccessorSource = propertySource.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
                    backingField = FirDefaultPropertyBackingField(
                        moduleData = baseModuleData,
                        origin = FirDeclarationOrigin.Source,
                        source = defaultAccessorSource,
                        annotations = parameterAnnotations.filter {
                            it.useSiteTarget == FIELD || it.useSiteTarget == PROPERTY_DELEGATE_FIELD
                        }.toMutableList(),
                        returnTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                        isVar = isVar,
                        propertySymbol = symbol,
                        status = status.copy(isLateInit = false),
                    )

                    this.status = status
                    getter = FirDefaultPropertyGetter(
                        defaultAccessorSource,
                        baseModuleData,
                        FirDeclarationOrigin.Source,
                        returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                        getVisibility(),
                        symbol,
                        isInline = hasModifier(INLINE_KEYWORD),
                    ).also { getter ->
                        getter.initContainingClassAttr()
                        getter.replaceAnnotations(parameterAnnotations.filterUseSiteTarget(PROPERTY_GETTER))
                    }
                    setter = if (isMutable) FirDefaultPropertySetter(
                        defaultAccessorSource,
                        baseModuleData,
                        FirDeclarationOrigin.Source,
                        returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                        getVisibility(),
                        symbol,
                        parameterAnnotations = parameterAnnotations.filterUseSiteTarget(SETTER_PARAMETER),
                        isInline = hasModifier(INLINE_KEYWORD),
                    ).also { setter ->
                        setter.initContainingClassAttr()
                        setter.replaceAnnotations(parameterAnnotations.filterUseSiteTarget(PROPERTY_SETTER))
                    } else null
                    annotations += parameterAnnotations.filterConstructorPropertyRelevantAnnotations(isMutable)

                    dispatchReceiverType = currentDispatchReceiverType()
                }.apply {
                    if (firParameter.isVararg) {
                        isFromVararg = true
                    }
                    firParameter.correspondingProperty = this
                    fromPrimaryConstructor = true
                }
            }
        }

        private fun FirDefaultPropertyAccessor.extractAnnotationsFrom(annotated: KtAnnotated) {
            annotated.extractAnnotationsTo(this)
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAnnotationContainer) {
            if (annotationEntries.isEmpty()) return
            val annotations = buildList {
                addAll(container.annotations)
                for (annotationEntry in annotationEntries) {
                    add(annotationEntry.convert())
                }
            }
            container.replaceAnnotations(annotations)
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAnnotationContainerBuilder) {
            for (annotationEntry in annotationEntries) {
                container.annotations += annotationEntry.convert<FirAnnotation>()
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(
            container: FirTypeParameterRefsOwnerBuilder,
            declarationSymbol: FirBasedSymbol<*>,
        ) {
            for (typeParameter in typeParameters) {
                container.typeParameters += extractTypeParameter(typeParameter, declarationSymbol)
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(
            container: FirTypeParametersOwnerBuilder,
            declarationSymbol: FirBasedSymbol<*>,
        ) {
            for (typeParameter in typeParameters) {
                container.typeParameters += extractTypeParameter(typeParameter, declarationSymbol)
            }
        }

        private fun extractTypeParameter(parameter: KtTypeParameter, declarationSymbol: FirBasedSymbol<*>): FirTypeParameter {
            val parameterName = parameter.nameAsSafeName
            return buildTypeParameter {
                source = parameter.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                name = parameterName
                symbol = FirTypeParameterSymbol()
                containingDeclarationSymbol = declarationSymbol
                variance = parameter.variance
                isReified = parameter.hasModifier(REIFIED_KEYWORD)
                parameter.extractAnnotationsTo(this)
                val extendsBound = parameter.extendsBound
                if (extendsBound != null) {
                    bounds += extendsBound.convert<FirTypeRef>()
                }
                val owner = parameter.getStrictParentOfType<KtTypeParameterListOwner>() ?: return@buildTypeParameter
                for (typeConstraint in owner.typeConstraints) {
                    val subjectName = typeConstraint.subjectTypeParameterName?.getReferencedNameAsName()

                    if (subjectName == parameterName) {
                        bounds += typeConstraint.boundTypeReference.toFirOrErrorType()
                    }

                    for (entry in typeConstraint.annotationEntries) {
                        annotations += buildErrorAnnotationCall {
                            source = entry.toFirSourceElement()
                            useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()
                            annotationTypeRef = entry.typeReference.toFirOrErrorType()
                            diagnostic = ConeSimpleDiagnostic(
                                "Type parameter annotations are not allowed inside where clauses", DiagnosticKind.AnnotationNotAllowed,
                            )
                            val name = (annotationTypeRef as? FirUserTypeRef)?.qualifier?.last()?.name
                                ?: Name.special("<no-annotation-name>")
                            calleeReference = buildSimpleNamedReference {
                                source = (entry.typeReference?.typeElement as? KtUserType)?.referenceExpression?.toFirSourceElement()
                                this.name = name
                            }
                            entry.extractArgumentsTo(this)
                            typeArguments.appendTypeArguments(entry.typeArguments)
                            containingDeclarationSymbol = this@buildTypeParameter.symbol
                        }
                    }
                }
                addDefaultBoundIfNecessary()
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: FirElement?): FirElement {
            throw AssertionError("KtTypeParameter should be process via extractTypeParameter")
        }

        private fun <T> KtTypeParameterListOwner.fillDanglingConstraintsTo(to: T) where T : FirDeclaration, T : FirTypeParameterRefsOwner {
            val typeParamNames = typeParameters.mapNotNull { it.nameAsName }.toSet()
            val result = typeConstraints.mapNotNull { constraint ->
                constraint.subjectTypeParameterName?.getReferencedNameAsName()?.let { name ->
                    if (!typeParamNames.contains(name)) {
                        DanglingTypeConstraint(name, constraint.subjectTypeParameterName!!.toFirSourceElement())
                    } else {
                        null
                    }
                }

            }
            if (result.isNotEmpty()) {
                to.danglingTypeConstraints = result
            }
        }

        private fun KtDeclarationWithBody.extractValueParametersTo(
            container: FirFunctionBuilder,
            functionSymbol: FirFunctionSymbol<*>,
            valueParameterDeclaration: ValueParameterDeclaration,
            defaultTypeRef: FirTypeRef? = null,
            additionalAnnotations: List<FirAnnotation> = emptyList(),
        ) {
            for (valueParameter in valueParameters) {
                container.valueParameters += valueParameter.toFirValueParameter(
                    defaultTypeRef, functionSymbol, valueParameterDeclaration, additionalAnnotations = additionalAnnotations
                )
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirCallBuilder) {
            val argumentList = buildArgumentList {
                source = valueArgumentList?.toFirSourceElement()
                for (argument in valueArguments) {
                    val argumentExpression =
                        buildOrLazyExpression((argument as? PsiElement)?.toFirSourceElement()) { argument.toFirExpression() }
                    arguments += when (argument) {
                        is KtLambdaArgument -> buildLambdaArgumentExpression {
                            source = argument.toFirSourceElement()
                            expression = argumentExpression
                        }
                        else -> argumentExpression
                    }
                }
            }
            container.argumentList = argumentList
        }

        /**
         * @param type the return type for new field.
         * In the case of null will be calculated inside [withContainerSymbol],
         * so it is crucial to decide to whom type annotation will be belonged
         */
        protected fun buildFieldForSupertypeDelegate(
            entry: KtDelegatedSuperTypeEntry,
            type: FirTypeRef?,
            fieldOrd: Int,
        ): FirField {
            val delegateSource = entry.toFirSourceElement(KtFakeSourceElementKind.ClassDelegationField)

            val delegateExpression = buildOrLazyExpression(delegateSource) {
                { entry.delegateExpression }
                    .toFirExpression("Should have delegate")
            }
            return buildField {
                source = delegateSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Synthetic.DelegateField
                name = NameUtils.delegateFieldName(fieldOrd)
                symbol = FirFieldSymbol(CallableId(this@PsiRawFirBuilder.context.currentClassId, name))
                returnTypeRef = type ?: withContainerSymbol(symbol) {
                    entry.typeReference.toFirOrErrorType()
                }

                isVar = false
                status = FirDeclarationStatusImpl(Visibilities.Private, Modality.FINAL)
                initializer = delegateExpression
                dispatchReceiverType = currentDispatchReceiverType()
            }
        }

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(
            container: FirClassBuilder,
            delegatedSelfTypeRef: FirTypeRef?,
            delegatedEnumSuperTypeRef: FirTypeRef?,
            classKind: ClassKind,
            containerTypeParameters: List<FirTypeParameterRef>,
            containingClassIsExpectClass: Boolean,
        ): Pair<FirTypeRef, Map<Int, FirFieldSymbol>?> {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            val allSuperTypeCallEntries = mutableListOf<Pair<KtSuperTypeCallEntry, FirTypeRef>>()
            var delegatedSuperTypeRef: FirTypeRef? = null
            val delegateFieldsMap = mutableMapOf<Int, FirFieldSymbol>()
            superTypeListEntries.forEachIndexed { index, superTypeListEntry ->
                when (superTypeListEntry) {
                    is KtSuperTypeEntry -> {
                        container.superTypeRefs += superTypeListEntry.typeReference.toFirOrErrorType()
                    }
                    is KtSuperTypeCallEntry -> {
                        delegatedSuperTypeRef = superTypeListEntry.calleeExpression.typeReference.toFirOrErrorType()
                        container.superTypeRefs += delegatedSuperTypeRef!!
                        superTypeCallEntry = superTypeListEntry
                        allSuperTypeCallEntries.add(superTypeListEntry to delegatedSuperTypeRef!!)
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        val type = superTypeListEntry.typeReference.toFirOrErrorType()
                        container.superTypeRefs += type

                        /**
                         * [FirAnnotationCall.containingDeclarationSymbol] have to be from [container], so we should calculate it here
                         */
                        val delegateField = buildFieldForSupertypeDelegate(superTypeListEntry, type, delegateFieldsMap.size)
                        delegateFieldsMap[index] = delegateField.symbol
                    }
                }
            }

            when {
                this is KtClass && classKind == ClassKind.ENUM_CLASS && superTypeCallEntry == null -> {
                    /*
                     * kotlin.Enum constructor has (name: String, ordinal: Int) signature,
                     *   so we should generate non-trivial constructors for enum and it's entry
                     *   for correct resolve of super constructor call or just call kotlin.Any constructor
                     *   and convert it to right call at backend, because of it doesn't affects frontend work
                     */
                    delegatedSuperTypeRef = buildResolvedTypeRef {
                        type = ConeClassLikeTypeImpl(
                            implicitEnumType.type.lookupTag,
                            delegatedSelfTypeRef?.coneType?.let { arrayOf(it) } ?: emptyArray(),
                            isNullable = false,
                        )
                    }
                    container.superTypeRefs += delegatedSuperTypeRef!!
                }
                this is KtClass && classKind == ClassKind.ANNOTATION_CLASS -> {
                    container.superTypeRefs += implicitAnnotationType
                    delegatedSuperTypeRef = implicitAnyType
                }
            }

            val isKotlinAny = this.isKotlinAny()
            val defaultDelegatedSuperTypeRef =
                when {
                    classKind == ClassKind.ENUM_ENTRY && this is KtClass -> delegatedEnumSuperTypeRef ?: implicitAnyType
                    container.superTypeRefs.isEmpty() && !isKotlinAny() -> implicitAnyType
                    else -> FirImplicitTypeRefImplWithoutSource
                }

            if (container.superTypeRefs.isEmpty() && !isKotlinAny) {
                container.superTypeRefs += implicitAnyType
                delegatedSuperTypeRef = implicitAnyType
            }

            // TODO: in case we have no primary constructor,
            // it may be not possible to determine delegated super type right here
            delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef

            // We are never here as part of enum entry
            val shouldGenerateImplicitPrimaryConstructor =
                !hasSecondaryConstructors() &&
                        !containingClassIsExpectClass &&
                        (this !is KtClass || !this.isInterface())

            val hasPrimaryConstructor = primaryConstructor != null || shouldGenerateImplicitPrimaryConstructor
            if (hasPrimaryConstructor || superTypeCallEntry != null) {
                val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                    superTypeCallEntry,
                    delegatedSuperTypeRef,
                    delegatedSelfTypeRef ?: delegatedSuperTypeRef!!,
                    owner = this,
                    containerTypeParameters,
                    allSuperTypeCallEntries,
                    containingClassIsExpectClass,
                    copyConstructedTypeRefWithImplicitSource = true,
                    isErrorConstructor = !hasPrimaryConstructor,
                    isImplicitlyActual = container.status.isActual && (container.status.isInline || classKind == ClassKind.ANNOTATION_CLASS),
                    isKotlinAny = isKotlinAny,
                )
                container.declarations += firPrimaryConstructor
            }

            delegateFieldsMap.values.mapTo(container.declarations) { it.fir }
            return delegatedSuperTypeRef!! to delegateFieldsMap.takeIf { it.isNotEmpty() }
        }

        private fun KtClassOrObject.isKotlinAny(): Boolean {
            if (nameAsName != StandardNames.FqNames.any.shortName()) return false
            val classOrObjectStub = stub

            if (classOrObjectStub != null) {
                val parentStub = classOrObjectStub.parentStub
                if (parentStub !is KotlinFileStub) return false
                return parentStub.getPackageFqName().pathSegments().singleOrNull() == StandardNames.BUILT_INS_PACKAGE_NAME
            } else {
                if (parent !is KtFile) return false
                return parent.findDescendantOfType<KtPackageDirective> { packageDirective ->
                    packageDirective.packageNames.singleOrNull()?.getReferencedNameAsName() == StandardNames.BUILT_INS_PACKAGE_NAME
                } != null
            }
        }

        /**
         * @param delegatedSuperTypeRef can be null if containingClassIsExpectClass is true
         */
        protected fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            ownerTypeParameters: List<FirTypeParameterRef>,
            allSuperTypeCallEntries: List<Pair<KtSuperTypeCallEntry, FirTypeRef>>,
            containingClassIsExpectClass: Boolean,
            copyConstructedTypeRefWithImplicitSource: Boolean,
            isErrorConstructor: Boolean = false,
            isImplicitlyActual: Boolean = false,
            isKotlinAny: Boolean = false,
        ): FirConstructor {
            val constructorSymbol = FirConstructorSymbol(callableIdForClassConstructor())
            withContainerSymbol(constructorSymbol) {
                val constructorSource = this?.toFirSourceElement()
                    ?: owner.toKtPsiSourceElement(KtFakeSourceElementKind.ImplicitConstructor)

                fun buildDelegatedCall(
                    superTypeCallEntry: KtSuperTypeCallEntry?,
                    delegatedTypeRef: FirTypeRef,
                ): FirDelegatedConstructorCall? {
                    val constructorCall = superTypeCallEntry?.toFirSourceElement()
                    val constructedTypeRef = if (copyConstructedTypeRefWithImplicitSource) {
                        delegatedTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
                    } else {
                        delegatedTypeRef
                    }
                    return buildOrLazyDelegatedConstructorCall(isThis = false, constructedTypeRef) {
                        buildDelegatedConstructorCall {
                            source = constructorCall ?: constructorSource.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                            this.constructedTypeRef = constructedTypeRef
                            isThis = false
                            calleeReference = buildExplicitSuperReference {
                                source =
                                    superTypeCallEntry?.calleeExpression?.toFirSourceElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                        ?: this@buildDelegatedConstructorCall.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                                superTypeRef = this@buildDelegatedConstructorCall.constructedTypeRef
                            }
                            superTypeCallEntry?.extractArgumentsTo(this)
                        }
                    }
                }

                val firDelegatedCall = runUnless(containingClassIsExpectClass || isKotlinAny) {
                    if (allSuperTypeCallEntries.size <= 1) {
                        buildDelegatedCall(superTypeCallEntry, delegatedSuperTypeRef!!)
                    } else {
                        buildMultiDelegatedConstructorCall {
                            allSuperTypeCallEntries.mapTo(delegatedConstructorCalls) { (superTypeCallEntry, delegatedTypeRef) ->
                                buildDelegatedCall(superTypeCallEntry, delegatedTypeRef)!!
                            }
                        }
                    }
                }

                // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
                fun defaultVisibility() = when {
                    owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) || owner is KtEnumEntry -> Visibilities.Private
                    owner.hasModifier(SEALED_KEYWORD) -> Visibilities.Protected
                    else -> Visibilities.Unknown
                }

                val explicitVisibility = this?.getVisibility()?.takeUnless { it == Visibilities.Unknown }
                val status = FirDeclarationStatusImpl(explicitVisibility ?: defaultVisibility(), Modality.FINAL).apply {
                    isExpect = this@toFirConstructor?.hasExpectModifier() == true || this@PsiRawFirBuilder.context.containerIsExpect
                    isActual = this@toFirConstructor?.hasActualModifier() == true || isImplicitlyActual

                    // a warning about inner script class is reported on the class itself
                    isInner = owner.parent.parent !is KtScript && owner.hasModifier(INNER_KEYWORD)
                    isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                    isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                }

                val builder = when {
                    this?.modifierList != null &&
                            getConstructorKeyword() == null -> createErrorConstructorBuilder(ConeMissingConstructorKeyword)
                    isErrorConstructor -> createErrorConstructorBuilder(ConeNoConstructorError)
                    else -> FirPrimaryConstructorBuilder()
                }

                builder.apply {
                    source = constructorSource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = delegatedSelfTypeRef
                    this.status = status
                    dispatchReceiverType = owner.obtainDispatchReceiverForConstructor()
                    symbol = constructorSymbol
                    delegatedConstructor = firDelegatedCall
                    typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                    this.contextReceivers.addAll(convertContextReceivers(owner.contextReceivers))
                    this@toFirConstructor?.extractAnnotationsTo(this)
                    this@toFirConstructor?.extractValueParametersTo(this, symbol, ValueParameterDeclaration.PRIMARY_CONSTRUCTOR)
                    this.body = null
                }

                return builder.build().apply {
                    containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                }
            }
        }

        private fun KtClassOrObject.obtainDispatchReceiverForConstructor(): ConeClassLikeType? =
            if (hasModifier(INNER_KEYWORD)) dispatchReceiverForInnerClassConstructor() else null

        override fun visitKtFile(file: KtFile, data: FirElement?): FirElement {
            context.packageFqName = when (mode) {
                BodyBuildingMode.NORMAL -> file.packageFqNameByTree
                BodyBuildingMode.LAZY_BODIES -> file.packageFqName
            }
            return buildFile {
                symbol = FirFileSymbol()
                source = file.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                name = file.name
                sourceFile = KtPsiSourceFile(file)
                sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)
                packageDirective = buildPackageDirective {
                    packageFqName = context.packageFqName
                    source = file.packageDirective?.toKtPsiSourceElement()
                }
                annotationsContainer = file.fileAnnotationList?.let {
                    buildFileAnnotationsContainer {
                        moduleData = baseModuleData
                        containingFileSymbol = this@buildFile.symbol
                        source = it.toKtPsiSourceElement()
                        withContainerSymbol(containingFileSymbol) {
                            for (annotationEntry in it.annotationEntries) {
                                annotations += annotationEntry.convert<FirAnnotation>()
                            }
                        }

                        annotations.ifEmpty {
                            resolvePhase = FirResolvePhase.BODY_RESOLVE
                        }
                    }
                }

                for (importDirective in file.importDirectives) {
                    imports += buildImport {
                        source = importDirective.toFirSourceElement()
                        importedFqName = importDirective.importedFqName
                        isAllUnder = importDirective.isAllUnder
                        aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                        aliasSource = importDirective.alias?.nameIdentifier?.toFirSourceElement()
                    }
                }

                if (file is KtCodeFragment) {
                    declarations += convertCodeFragment(file)
                } else {
                    for (declaration in file.declarations) {
                        declarations += when (declaration) {
                            is KtScript -> {
                                requireWithAttachment(
                                    file.declarations.size == 1,
                                    message = { "Expect the script to be the only declaration in the file" },
                                ) {
                                    withEntry("fileName", file.name)
                                }

                                convertScript(declaration, name, sourceFile) {
                                    for (configurator in baseSession.extensionService.scriptConfigurators) {
                                        with(configurator) { configureContainingFile(this@buildFile) }
                                    }
                                }
                            }
                            is KtDestructuringDeclaration -> buildErrorTopLevelDestructuringDeclaration(declaration.toFirSourceElement())
                            else -> declaration.convert()
                        }
                    }

                    for (danglingModifierList in file.danglingModifierLists) {
                        declarations += buildErrorTopLevelDeclarationForDanglingModifierList(danglingModifierList)
                    }
                }
            }
        }

        protected fun configureScriptDestructuringDeclarationEntry(declaration: FirVariable, container: FirVariable) {
            (declaration as FirProperty).destructuringDeclarationContainerVariable = container.symbol
        }

        protected fun buildScriptDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration): FirVariable {
            val initializer = destructuringDeclaration.initializer
            val firInitializer = buildOrLazyExpression(initializer?.toFirSourceElement()) {
                initializer.toFirExpression { ConeSyntaxDiagnostic("Initializer required for destructuring declaration") }
            }

            val destructuringContainerVar = generateTemporaryVariable(
                moduleData = baseModuleData,
                source = destructuringDeclaration.toFirSourceElement(),
                specialName = "destruct",
                initializer = firInitializer,
                origin = FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer,
                extractAnnotationsTo = { extractAnnotationsTo(it) },
            ).apply {
                isDestructuringDeclarationContainerVariable = true
            }

            return destructuringContainerVar
        }

        private fun convertScript(
            script: KtScript,
            fileName: String,
            sourceFile: KtSourceFile?,
            setup: FirScriptBuilder.() -> Unit = {},
        ): FirScript {
            val scriptName = Name.special("<script-$fileName>")
            val scriptSymbol = FirScriptSymbol(context.packageFqName.child(scriptName))

            return buildScript {
                source = script.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                name = scriptName
                symbol = scriptSymbol

                val scriptDeclarationsIter = script.declarations.listIterator()
                withContainerSymbol(symbol) {
                    while (scriptDeclarationsIter.hasNext()) {
                        val declaration = scriptDeclarationsIter.next()
                        val isLast = !scriptDeclarationsIter.hasNext()
                        when (declaration) {
                            is KtScriptInitializer -> {
                                val initializer = buildAnonymousInitializer(
                                    initializer = declaration,
                                    containingDeclarationSymbol = scriptSymbol,
                                    // the last one need to be analyzed in script configurator to decide on result property
                                    // therefore no lazy conversion in this case
                                    allowLazyBody = !isLast,
                                )

                                declarations.add(initializer)
                            }
                            is KtDestructuringDeclaration -> {
                                val destructuringContainerVar = buildScriptDestructuringDeclaration(declaration)
                                declarations.add(destructuringContainerVar)

                                declarations.addDestructuringVariables(
                                    moduleData,
                                    declaration,
                                    destructuringContainerVar,
                                    tmpVariable = false,
                                    localEntries = false,
                                ) {
                                    configureScriptDestructuringDeclarationEntry(it, destructuringContainerVar)
                                }
                            }
                            else -> {
                                val firStatement = declaration.toFirStatement()
                                if (firStatement is FirDeclaration) {
                                    declarations.add(firStatement)
                                } else {
                                    error("unexpected declaration type in script")
                                }
                            }
                        }
                    }
                    setup()
                    if (sourceFile != null) {
                        for (configurator in baseSession.extensionService.scriptConfigurators) {
                            with(configurator) { configure(sourceFile) }
                        }
                    }
                }
            }
        }

        private fun convertCodeFragment(file: KtCodeFragment): FirCodeFragment {
            return buildCodeFragment {
                source = file.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                symbol = FirCodeFragmentSymbol()
                withContainerSymbol(symbol) {
                    block = buildOrLazyBlock {
                        when (file) {
                            is KtExpressionCodeFragment -> file.getContentElement()?.toFirBlock() ?: buildEmptyExpressionBlock()
                            is KtBlockCodeFragment -> configureBlockWithoutBuilding(file.getContentElement()).build()
                            is KtTypeCodeFragment -> convertTypeCodeFragmentBlock(file)
                            else -> error("Unexpected code fragment type: ${file::class}")
                        }
                    }
                }
            }
        }

        private fun convertTypeCodeFragmentBlock(file: KtTypeCodeFragment): FirBlock {
            return buildBlock {
                statements += buildTypeOperatorCall {
                    operation = FirOperation.IS
                    conversionTypeRef = file.getContentElement().toFirOrErrorType()
                    argumentList = buildUnaryArgumentList(
                        buildLiteralExpression(source = null, ConstantValueKind.Null, value = null, setType = false)
                    )
                }
            }
        }

        override fun visitScript(script: KtScript, data: FirElement?): FirElement {
            val ktFile = script.containingKtFile
            val fileName = ktFile.name
            val fileForSource = (data as? FirScript)?.psi?.containingFile as? KtFile ?: ktFile
            return convertScript(script, fileName, KtPsiSourceFile(fileForSource))
        }

        protected fun KtEnumEntry.toFirEnumEntry(
            delegatedEnumSelfTypeRef: FirResolvedTypeRef,
            ownerClassHasDefaultConstructor: Boolean,
        ): FirDeclaration {
            val ktEnumEntry = this@toFirEnumEntry
            val containingClassIsExpectClass = hasExpectModifier() || this@PsiRawFirBuilder.context.containerIsExpect
            val enumSymbol = FirEnumEntrySymbol(callableIdForName(nameAsSafeName))
            return withContainerSymbol(enumSymbol) {
                buildEnumEntry {
                    source = toFirSourceElement()
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = delegatedEnumSelfTypeRef
                    name = nameAsSafeName
                    status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                        isStatic = true
                        isExpect = containingClassIsExpectClass
                    }
                    symbol = enumSymbol
                    if (ownerClassHasDefaultConstructor && ktEnumEntry.initializerList == null &&
                        ktEnumEntry.annotationEntries.isEmpty() && ktEnumEntry.body == null
                    ) {
                        return@buildEnumEntry
                    }
                    extractAnnotationsTo(this)
                    initializer = buildOrLazyExpression(toFirSourceElement(KtFakeSourceElementKind.EnumInitializer)) {
                        withChildClassName(nameAsSafeName, isExpect = false) {
                            buildAnonymousObjectExpression {
                                val enumEntrySource = toFirSourceElement(KtFakeSourceElementKind.EnumInitializer)
                                source = enumEntrySource
                                anonymousObject = buildAnonymousObject {
                                    source = enumEntrySource
                                    moduleData = baseModuleData
                                    origin = FirDeclarationOrigin.Source
                                    classKind = ClassKind.ENUM_ENTRY
                                    scopeProvider = this@PsiRawFirBuilder.baseScopeProvider
                                    symbol = FirAnonymousObjectSymbol(this@PsiRawFirBuilder.context.packageFqName)
                                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)

                                    val delegatedEntrySelfType = buildResolvedTypeRef {
                                        type =
                                            ConeClassLikeTypeImpl(
                                                this@buildAnonymousObject.symbol.toLookupTag(),
                                                emptyArray(),
                                                isNullable = false
                                            )
                                    }
                                    registerSelfType(delegatedEntrySelfType)

                                    superTypeRefs += delegatedEnumSelfTypeRef
                                    val superTypeCallEntry = superTypeListEntries.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                                    val correctedEnumSelfTypeRef = buildResolvedTypeRef {
                                        source = superTypeCallEntry?.calleeExpression?.typeReference?.toFirSourceElement()
                                        type = delegatedEnumSelfTypeRef.type
                                    }
                                    declarations += primaryConstructor.toFirConstructor(
                                        superTypeCallEntry,
                                        correctedEnumSelfTypeRef,
                                        delegatedEntrySelfType,
                                        owner = ktEnumEntry,
                                        typeParameters,
                                        allSuperTypeCallEntries = emptyList(),
                                        containingClassIsExpectClass,
                                        copyConstructedTypeRefWithImplicitSource = true,
                                    )
                                    // Use ANONYMOUS_OBJECT_NAME for the owner class id for enum entry declarations (see KT-42351)
                                    withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
                                        for (declaration in ktEnumEntry.declarations) {
                                            declarations += declaration.toFirDeclaration(
                                                correctedEnumSelfTypeRef,
                                                delegatedSelfType = delegatedEntrySelfType,
                                                ktEnumEntry,
                                                ownerClassBuilder = this,
                                                ownerTypeParameters = emptyList()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.apply {
                    containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                }
            }
        }

        private fun convertContextReceivers(receivers: List<KtContextReceiver>): List<FirContextReceiver> {
            return receivers.map { contextReceiverElement ->
                buildContextReceiver {
                    this.source = contextReceiverElement.toFirSourceElement()
                    this.customLabelName = contextReceiverElement.labelNameAsName()
                    this.labelNameFromTypeRef = contextReceiverElement.typeReference()?.nameForReceiverLabel()?.let(Name::identifier)

                    contextReceiverElement.typeReference()?.toFirType()?.let {
                        this.typeRef = it
                    }
                }
            }
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject, data: FirElement?): FirElement {
            val isLocalWithinParent = classOrObject.parent !is KtClassBody && classOrObject.isLocal
            val classIsExpect = classOrObject.hasExpectModifier() || context.containerIsExpect
            val sourceElement = classOrObject.toFirSourceElement()
            return withChildClassName(
                classOrObject.nameAsSafeName,
                isExpect = classIsExpect,
                forceLocalContext = isLocalWithinParent,
            ) {
                val classSymbol = FirRegularClassSymbol(context.currentClassId)
                withContainerSymbol(classSymbol) {
                    val isLocal = context.inLocalContext
                    val classKind = when (classOrObject) {
                        is KtObjectDeclaration -> ClassKind.OBJECT
                        is KtClass -> when {
                            classOrObject.isInterface() -> ClassKind.INTERFACE
                            classOrObject.isEnum() -> ClassKind.ENUM_CLASS
                            classOrObject.isAnnotation() -> ClassKind.ANNOTATION_CLASS
                            else -> ClassKind.CLASS
                        }
                        else -> throw AssertionError("Unexpected class or object: ${classOrObject.text}")
                    }
                    val status = FirDeclarationStatusImpl(
                        if (isLocal) Visibilities.Local else classOrObject.getVisibility(publicByDefault = true),
                        classOrObject.modality,
                    ).apply {
                        isExpect = classIsExpect
                        isActual = classOrObject.hasActualModifier()
                        isInner = classOrObject.hasModifier(INNER_KEYWORD) && classOrObject.parent.parent !is KtScript
                        isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
                        isData = classOrObject.hasModifier(DATA_KEYWORD)
                        isInline = classOrObject.hasModifier(INLINE_KEYWORD) || classOrObject.hasModifier(VALUE_KEYWORD)
                        isFun = classOrObject.hasModifier(FUN_KEYWORD)
                        isExternal = classOrObject.hasModifier(EXTERNAL_KEYWORD)
                    }

                    withCapturedTypeParameters(status.isInner || isLocal, sourceElement, listOf()) {
                        var delegatedFieldsMap: Map<Int, FirFieldSymbol>?
                        buildRegularClass {
                            source = sourceElement
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            name = classOrObject.nameAsSafeName
                            this.status = status
                            this.classKind = classKind
                            scopeProvider = baseScopeProvider
                            symbol = classSymbol

                            classOrObject.extractAnnotationsTo(this)
                            classOrObject.extractTypeParametersTo(this, symbol)

                            context.appendOuterTypeParameters(ignoreLastLevel = true, typeParameters)
                            context.pushFirTypeParameters(
                                status.isInner || isLocal,
                                typeParameters.subList(0, classOrObject.typeParameters.size)
                            )

                            val delegatedSelfType = classOrObject.toDelegatedSelfType(this)
                            registerSelfType(delegatedSelfType)

                            val (delegatedSuperType, extractedDelegatedFieldsMap) = classOrObject.extractSuperTypeListEntriesTo(
                                this,
                                delegatedSelfType,
                                null,
                                classKind,
                                typeParameters,
                                containingClassIsExpectClass = classIsExpect
                            )
                            delegatedFieldsMap = extractedDelegatedFieldsMap

                            val primaryConstructor = classOrObject.primaryConstructor
                            val firPrimaryConstructor = declarations.firstOrNull { it is FirConstructor } as? FirConstructor
                            if (primaryConstructor != null && firPrimaryConstructor != null) {
                                primaryConstructor.valueParameters.zip(
                                    firPrimaryConstructor.valueParameters
                                ).forEach { (ktParameter, firParameter) ->
                                    if (ktParameter.hasValOrVar()) {
                                        addDeclaration(ktParameter.toFirProperty(firParameter))
                                    }
                                }
                            }

                            for (declaration in classOrObject.declarations) {
                                addDeclaration(
                                    declaration.toFirDeclaration(
                                        delegatedSuperType,
                                        delegatedSelfType,
                                        classOrObject,
                                        this,
                                        typeParameters
                                    )
                                )
                            }
                            for (danglingModifier in classOrObject.body?.danglingModifierLists ?: emptyList()) {
                                addDeclaration(
                                    buildErrorTopLevelDeclarationForDanglingModifierList(danglingModifier).apply {
                                        containingClassAttr = currentDispatchReceiverType()?.lookupTag
                                    }
                                )
                            }

                            if (classOrObject.hasModifier(DATA_KEYWORD) && firPrimaryConstructor != null) {
                                val zippedParameters =
                                    classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() } zip declarations.filterIsInstance<FirProperty>()
                                DataClassMembersGenerator(
                                    classOrObject,
                                    this,
                                    zippedParameters,
                                    context.packageFqName,
                                    context.className,
                                    createClassTypeRefWithSourceKind = { firPrimaryConstructor.returnTypeRef.copyWithNewSourceKind(it) },
                                    createParameterTypeRefWithSourceKind = { property, newKind ->
                                        property.returnTypeRef.copyWithNewSourceKind(newKind)
                                    },
                                    addValueParameterAnnotations = {
                                        addAnnotationsFrom(
                                            it as KtParameter,
                                            isFromPrimaryConstructor = true,
                                        )
                                    },
                                ).generate()
                            }

                            if (classOrObject.hasModifier(ENUM_KEYWORD)) {
                                generateValuesFunction(
                                    baseModuleData,
                                    context.packageFqName,
                                    context.className,
                                    classIsExpect
                                )
                                generateValueOfFunction(
                                    baseModuleData, context.packageFqName, context.className,
                                    classIsExpect
                                )
                                generateEntriesGetter(
                                    baseModuleData, context.packageFqName, context.className,
                                    classIsExpect
                                )
                            }

                            initCompanionObjectSymbolAttr()

                            context.popFirTypeParameters()
                            contextReceivers.addAll(convertContextReceivers(classOrObject.contextReceivers))
                        }.also {
                            it.delegateFieldsMap = delegatedFieldsMap
                        }
                    }.also {
                        classOrObject.fillDanglingConstraintsTo(it)
                    }
                }
            }.also {
                if (classOrObject.parent is KtClassBody) {
                    it.initContainingClassForLocalAttr()
                }
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: FirElement?): FirElement {
            return withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
                var delegatedFieldsMap: Map<Int, FirFieldSymbol>?
                buildAnonymousObjectExpression {
                    source = expression.toFirSourceElement()
                    anonymousObject = buildAnonymousObject {
                        val objectDeclaration = expression.objectDeclaration
                        source = objectDeclaration.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        classKind = ClassKind.CLASS
                        scopeProvider = baseScopeProvider
                        symbol = FirAnonymousObjectSymbol(context.packageFqName)
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                        context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
                        val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                        registerSelfType(delegatedSelfType)
                        objectDeclaration.extractAnnotationsTo(this)
                        val (delegatedSuperType, extractedDelegatedFieldsMap) = objectDeclaration.extractSuperTypeListEntriesTo(
                            this,
                            delegatedSelfType,
                            null,
                            ClassKind.CLASS,
                            containerTypeParameters = emptyList(),
                            containingClassIsExpectClass = false
                        )
                        delegatedFieldsMap = extractedDelegatedFieldsMap

                        for (declaration in objectDeclaration.declarations) {
                            declarations += declaration.toFirDeclaration(
                                delegatedSuperType,
                                delegatedSelfType,
                                owner = objectDeclaration,
                                ownerClassBuilder = this,
                                ownerTypeParameters = emptyList()
                            )
                        }

                        for (danglingModifier in objectDeclaration.body?.danglingModifierLists ?: emptyList()) {
                            declarations += buildErrorTopLevelDeclarationForDanglingModifierList(danglingModifier).apply {
                                containingClassAttr = currentDispatchReceiverType()?.lookupTag
                            }
                        }
                    }.also {
                        it.delegateFieldsMap = delegatedFieldsMap
                    }
                }
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: FirElement?): FirElement {
            val typeAliasIsExpect = typeAlias.hasExpectModifier() || context.containerIsExpect
            return withChildClassName(typeAlias.nameAsSafeName, isExpect = typeAliasIsExpect) {
                buildTypeAlias {
                    symbol = FirTypeAliasSymbol(context.currentClassId)
                    withContainerSymbol(symbol) {
                        source = typeAlias.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = typeAlias.nameAsSafeName
                        val isLocal = context.inLocalContext
                        status = FirDeclarationStatusImpl(
                            if (isLocal) Visibilities.Local else typeAlias.getVisibility(publicByDefault = true),
                            Modality.FINAL
                        ).apply {
                            isExpect = typeAliasIsExpect
                            isActual = typeAlias.hasActualModifier()
                        }
                        expandedTypeRef = typeAlias.getTypeReference().toFirOrErrorType()
                        typeAlias.extractAnnotationsTo(this)
                        typeAlias.extractTypeParametersTo(this, symbol)
                    }
                }
            }
        }

        override fun visitNamedFunction(function: KtNamedFunction, data: FirElement?): FirElement {
            val isAnonymousFunction = function.isAnonymous
            val isLocalFunction = function.isLocal
            val functionSymbol: FirFunctionSymbol<*> = if (isAnonymousFunction) {
                FirAnonymousFunctionSymbol()
            } else {
                FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName))
            }

            withContainerSymbol(functionSymbol, isLocalFunction) {
                val typeReference = function.typeReference
                val returnType = if (function.hasBlockBody()) {
                    typeReference.toFirOrUnitType()
                } else {
                    typeReference.toFirOrImplicitType()
                }

                val receiverType = function.receiverTypeReference?.toFirType()
                val labelName: String?

                val functionBuilder = if (isAnonymousFunction) {
                    FirAnonymousFunctionBuilder().apply {
                        receiverParameter = receiverType?.convertToReceiverParameter()
                        symbol = functionSymbol as FirAnonymousFunctionSymbol
                        isLambda = false
                        hasExplicitParameterList = true
                        label = context.getLastLabel(function)
                        labelName = label?.name ?: context.calleeNamesForLambda.lastOrNull()?.identifier
                        if (function.hasModifier(SUSPEND_KEYWORD)) {
                            status = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_SUSPEND_FUNCTION_EXPRESSION
                        }
                    }
                } else {
                    FirSimpleFunctionBuilder().apply {
                        receiverParameter = receiverType?.convertToReceiverParameter()
                        name = function.nameAsSafeName
                        labelName = context.getLastLabel(function)?.name ?: runIf(!name.isSpecial) { name.identifier }
                        symbol = functionSymbol as FirNamedFunctionSymbol
                        dispatchReceiverType = runIf(!isLocalFunction) { currentDispatchReceiverType() }
                        status = FirDeclarationStatusImpl(
                            if (isLocalFunction) Visibilities.Local else function.getVisibility(),
                            function.modality,
                        ).apply {
                            isExpect = function.hasExpectModifier() || context.containerIsExpect
                            isActual = function.hasActualModifier()
                            isOverride = function.hasModifier(OVERRIDE_KEYWORD)
                            isOperator = function.hasModifier(OPERATOR_KEYWORD)
                            isInfix = function.hasModifier(INFIX_KEYWORD)
                            isInline = function.hasModifier(INLINE_KEYWORD)
                            isTailRec = function.hasModifier(TAILREC_KEYWORD)
                            isExternal = function.hasModifier(EXTERNAL_KEYWORD)
                            isSuspend = function.hasModifier(SUSPEND_KEYWORD)
                        }

                        contextReceivers.addAll(convertContextReceivers(function.contextReceivers))
                    }
                }

                val target = FirFunctionTarget(labelName, isLambda = false)
                val functionSource = function.toFirSourceElement()
                val firFunction = functionBuilder.apply {
                    source = functionSource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = returnType

                    context.firFunctionTargets += target
                    function.extractAnnotationsTo(this)
                    if (this is FirSimpleFunctionBuilder) {
                        function.extractTypeParametersTo(this, symbol)
                    }
                    for (valueParameter in function.valueParameters) {
                        valueParameters += valueParameter.toFirValueParameter(
                            null,
                            functionSymbol,
                            if (isAnonymousFunction) ValueParameterDeclaration.LAMBDA else ValueParameterDeclaration.FUNCTION,
                        )
                    }
                    val actualTypeParameters = if (this is FirSimpleFunctionBuilder)
                        this.typeParameters
                    else
                        listOf()
                    withCapturedTypeParameters(true, functionSource, actualTypeParameters) {
                        val outerContractDescription = function.obtainContractDescription()
                        val (body, innerContractDescription) = withForcedLocalContext {
                            function.buildFirBody()
                        }
                        this.body = body
                        val contractDescription = outerContractDescription ?: innerContractDescription
                        contractDescription?.let {
                            if (this is FirSimpleFunctionBuilder) {
                                this.contractDescription = it
                            } else if (this is FirAnonymousFunctionBuilder) {
                                this.contractDescription = it
                            }
                        }
                    }
                    context.firFunctionTargets.removeLast()
                }.build().also {
                    bindFunctionTarget(target, it)
                    if (it is FirSimpleFunction) {
                        function.fillDanglingConstraintsTo(it)
                    }
                }

                return if (firFunction is FirAnonymousFunction) {
                    buildAnonymousFunctionExpression {
                        source = functionSource
                        anonymousFunction = firFunction
                    }
                } else {
                    firFunction
                }
            }
        }

        private fun KtDeclarationWithBody.obtainContractDescription(): FirContractDescription? {
            return when (val description = contractDescription) {
                null -> null
                else -> buildRawContractDescription {
                    source = description.toFirSourceElement()
                    description.extractRawEffects(rawEffects)
                }
            }
        }

        private fun KtContractEffectList.extractRawEffects(destination: MutableList<FirExpression>) {
            getContractEffects().mapTo(destination) { effect ->
                buildOrLazyExpression(effect.toFirSourceElement()) {
                    effect.getExpression().accept(this@Visitor, null) as FirExpression
                }
            }
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: FirElement?): FirElement {
            val literal = expression.functionLiteral
            val literalSource = literal.toFirSourceElement()

            val target: FirFunctionTarget
            val anonymousFunction = buildAnonymousFunction {
                source = literalSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = FirImplicitTypeRefImplWithoutSource
                receiverParameter = literalSource.asReceiverParameter()
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true
                hasExplicitParameterList = expression.functionLiteral.arrow != null

                val destructuringVariables = mutableListOf<FirStatement>()
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val name = SpecialNames.DESTRUCT
                        val multiParameter = buildValueParameter {
                            source = valueParameter.toFirSourceElement()
                            containingFunctionSymbol = this@buildAnonymousFunction.symbol
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = valueParameter.typeReference.toFirOrImplicitType()
                            this.name = name
                            symbol = FirValueParameterSymbol(name)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                        destructuringVariables.addDestructuringVariables(
                            baseModuleData,
                            multiDeclaration,
                            multiParameter,
                            tmpVariable = false,
                            localEntries = true,
                        )
                        multiParameter
                    } else {
                        val typeRef = valueParameter.typeReference.toFirOrImplicitType()
                        valueParameter.toFirValueParameter(typeRef, symbol, ValueParameterDeclaration.LAMBDA)
                    }
                }
                val expressionSource = expression.toFirSourceElement()
                label = context.getLastLabel(expression) ?: context.calleeNamesForLambda.lastOrNull()?.let {
                    buildLabel {
                        source = expressionSource.fakeElement(KtFakeSourceElementKind.GeneratedLambdaLabel)
                        name = it.asString()
                    }
                }
                target = FirFunctionTarget(label?.name, isLambda = true).also {
                    context.firFunctionTargets += it
                }
                val ktBody = literal.bodyExpression
                body = withForcedLocalContext {
                    if (ktBody == null) {
                        val errorExpression = buildErrorExpression(source, ConeSyntaxDiagnostic("Lambda has no body"))
                        FirSingleExpressionBlock(errorExpression.toReturn())
                    } else {
                        val kind = runIf(destructuringVariables.isNotEmpty()) {
                            KtFakeSourceElementKind.LambdaDestructuringBlock
                        }
                        val bodyBlock = configureBlockWithoutBuilding(ktBody, kind).apply {
                            statements.firstOrNull()?.let {
                                if (it.isContractBlockFirCheck()) {
                                    this@buildAnonymousFunction.contractDescription = it.toLegacyRawContractDescription()
                                    statements[0] = FirContractCallBlock(it)
                                }
                            }

                            if (statements.isEmpty()) {
                                statements.add(
                                    buildReturnExpression {
                                        source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody)
                                        this.target = target
                                        result = buildUnitExpression {
                                            source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitUnit.LambdaCoercion)
                                        }
                                    }
                                )
                            }
                        }.build()

                        if (destructuringVariables.isNotEmpty()) {
                            // Destructured variables must be in a separate block so that they can be shadowed.
                            buildBlock {
                                source = bodyBlock.source?.realElement()
                                statements.addAll(destructuringVariables)
                                statements.add(bodyBlock)
                            }
                        } else {
                            bodyBlock
                        }
                    }
                }
                context.firFunctionTargets.removeLast()
            }.also {
                bindFunctionTarget(target, it)
            }
            return buildAnonymousFunctionExpression {
                source = expression.toKtPsiSourceElement()
                this.anonymousFunction = anonymousFunction
            }
        }

        protected fun KtSecondaryConstructor.toFirConstructor(
            delegatedTypeRef: FirTypeRef,
            selfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            ownerTypeParameters: List<FirTypeParameterRef>,
        ): FirConstructor {
            val target = FirFunctionTarget(labelName = null, isLambda = false)
            return buildConstructor {
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                withContainerSymbol(symbol) {
                    source = this@toFirConstructor.toFirSourceElement()
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = selfTypeRef
                    val explicitVisibility = getVisibility()
                    status = FirDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
                        isExpect = hasExpectModifier() || this@PsiRawFirBuilder.context.containerIsExpect
                        isActual = hasActualModifier()
                        isInner = owner.hasModifier(INNER_KEYWORD)
                        isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                        isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                    }
                    dispatchReceiverType = owner.obtainDispatchReceiverForConstructor()
                    contextReceivers.addAll(convertContextReceivers(owner.contextReceivers))
                    if (!owner.hasModifier(EXTERNAL_KEYWORD) || isExplicitDelegationCall()) {
                        delegatedConstructor = buildOrLazyDelegatedConstructorCall(
                            isThis = isDelegatedCallToThis(),
                            constructedTypeRef = delegatedTypeRef,
                        ) {
                            getDelegationCall().convert(delegatedTypeRef)
                        }
                    }
                    this@PsiRawFirBuilder.context.firFunctionTargets += target
                    extractAnnotationsTo(this)
                    typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                    extractValueParametersTo(this, symbol, ValueParameterDeclaration.FUNCTION)

                    val (body, contractDescription) = withForcedLocalContext {
                        buildFirBody()
                    }
                    contractDescription?.let { this.contractDescription = it }
                    this.body = body
                    this@PsiRawFirBuilder.context.firFunctionTargets.removeLast()
                }
            }.also {
                it.containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                bindFunctionTarget(target, it)
            }
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedType: FirTypeRef,
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis //|| (isImplicit && hasPrimaryConstructor)
            val source = if (isImplicit) {
                this.toFirSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
            } else {
                this.toFirSourceElement()
            }
            return buildDelegatedConstructorCall {
                this.source = source
                constructedTypeRef = delegatedType.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
                this.isThis = isThis
                val calleeKind =
                    if (isImplicit) KtFakeSourceElementKind.ImplicitConstructor else KtFakeSourceElementKind.DelegatingConstructorCall
                val calleeSource = this@convert.calleeExpression?.toFirSourceElement(calleeKind)
                    ?: source.fakeElement(calleeKind)
                this.calleeReference = if (isThis) {
                    buildExplicitThisReference {
                        this.source = calleeSource
                    }
                } else {
                    buildExplicitSuperReference {
                        this.source = calleeSource
                        this.superTypeRef = this@buildDelegatedConstructorCall.constructedTypeRef
                    }
                }
                extractArgumentsTo(this)
            }
        }

        private fun KtDeclarationWithInitializer.toInitializerExpression() =
            runIf(hasInitializer()) {
                this@PsiRawFirBuilder.context.calleeNamesForLambda += null

                val expression = buildOrLazyExpression(null) {
                    initializer.toFirExpression("Should have initializer")
                }

                this@PsiRawFirBuilder.context.calleeNamesForLambda.removeLast()
                expression
            }

        private fun <T> KtProperty.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            context: Context<T>,
        ): FirProperty {
            val propertyName = nameAsSafeName
            val propertySymbol = if (isLocal) {
                FirPropertySymbol(propertyName)
            } else {
                FirPropertySymbol(callableIdForName(propertyName))
            }

            withContainerSymbol(propertySymbol, isLocal) {
                val propertyType = typeReference.toFirOrImplicitType()
                val isVar = isVar
                val propertyInitializer = toInitializerExpression()

                val propertySource = toFirSourceElement()

                return buildProperty {
                    source = propertySource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    returnTypeRef = propertyType
                    name = propertyName
                    this.isVar = isVar

                    receiverParameter = receiverTypeReference?.toFirType()?.convertToReceiverParameter()
                    initializer = propertyInitializer

                    val propertyAnnotations = mutableListOf<FirAnnotationCall>()
                    for (annotationEntry in annotationEntries) {
                        propertyAnnotations += annotationEntry.convert<FirAnnotationCall>()
                    }
                    if (this@toFirProperty.isLocal) {
                        isLocal = true
                        symbol = propertySymbol

                        extractTypeParametersTo(this, symbol)
                        backingField = this@toFirProperty.fieldDeclaration.toFirBackingField(
                            this@toFirProperty,
                            propertySymbol = symbol,
                            propertyType,
                            emptyList(),
                        )

                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL).apply {
                            isLateInit = hasModifier(LATEINIT_KEYWORD)
                        }

                        if (hasDelegate()) {
                            fun extractDelegateExpression() =
                                this@toFirProperty.delegate?.expression.toFirExpression("Incorrect delegate expression")

                            val delegateBuilder = FirWrappedDelegateExpressionBuilder().apply {
                                val delegateFirExpression = extractDelegateExpression()
                                source = delegateFirExpression.source?.fakeElement(KtFakeSourceElementKind.WrappedDelegate)
                                    ?: this@toFirProperty.delegate?.toFirSourceElement(KtFakeSourceElementKind.WrappedDelegate)
                                expression = delegateFirExpression
                            }

                            generateAccessorsByDelegate(
                                delegateBuilder,
                                baseModuleData,
                                ownerRegularOrAnonymousObjectSymbol = null,
                                context = context,
                                isExtension = false,
                            )
                        }
                    } else {
                        isLocal = false
                        symbol = propertySymbol
                        dispatchReceiverType = currentDispatchReceiverType()
                        extractTypeParametersTo(this, symbol)
                        withCapturedTypeParameters(true, propertySource, this.typeParameters) {
                            backingField = this@toFirProperty.fieldDeclaration.toFirBackingField(
                                this@toFirProperty,
                                propertySymbol = symbol,
                                propertyType,
                                propertyAnnotations.filter { it.useSiteTarget == FIELD || it.useSiteTarget == PROPERTY_DELEGATE_FIELD },
                            )

                            getter = this@toFirProperty.getter.toFirPropertyAccessor(
                                this@toFirProperty,
                                propertyType,
                                propertySymbol = symbol,
                                isGetter = true,
                                accessorAnnotationsFromProperty = propertyAnnotations.filterUseSiteTarget(PROPERTY_GETTER),
                                parameterAnnotationsFromProperty = emptyList()
                            )

                            setter = this@toFirProperty.setter.toFirPropertyAccessor(
                                this@toFirProperty,
                                propertyType,
                                propertySymbol = symbol,
                                isGetter = false,
                                accessorAnnotationsFromProperty = propertyAnnotations.filterUseSiteTarget(PROPERTY_SETTER),
                                parameterAnnotationsFromProperty = propertyAnnotations.filterUseSiteTarget(SETTER_PARAMETER)
                            )

                            status = FirDeclarationStatusImpl(getVisibility(), modality).apply {
                                isExpect = hasExpectModifier() || this@PsiRawFirBuilder.context.containerIsExpect
                                isActual = hasActualModifier()
                                isOverride = hasModifier(OVERRIDE_KEYWORD)
                                isConst = hasModifier(CONST_KEYWORD)
                                isLateInit = hasModifier(LATEINIT_KEYWORD)
                                isExternal = hasModifier(EXTERNAL_KEYWORD)
                            }

                            val psiPropertyDelegate = this@toFirProperty.delegate
                            if (psiPropertyDelegate != null) {
                                fun extractDelegateExpression(): FirExpression {
                                    return buildOrLazyExpression(psiPropertyDelegate.expression?.toFirSourceElement(KtFakeSourceElementKind.WrappedDelegate)) {
                                        psiPropertyDelegate.expression?.toFirExpression("Should have delegate") ?: buildErrorExpression {
                                            diagnostic = ConeSimpleDiagnostic("Should have delegate", DiagnosticKind.ExpressionExpected)
                                        }
                                    }
                                }

                                val delegateBuilder = FirWrappedDelegateExpressionBuilder().apply {
                                    val delegateExpression = extractDelegateExpression()
                                    source = (psiPropertyDelegate.expression ?: psiPropertyDelegate)
                                        .toFirSourceElement(KtFakeSourceElementKind.WrappedDelegate)

                                    expression = delegateExpression
                                }

                                val (lazyDelegateExpression: FirLazyExpression?, lazyBody: FirLazyBlock?) = buildOrLazy(
                                    build = { null to null },
                                    lazy = { buildLazyExpression { source = delegateBuilder.source } to buildLazyBlock() },
                                )

                                generateAccessorsByDelegate(
                                    delegateBuilder,
                                    baseModuleData,
                                    ownerRegularOrAnonymousObjectSymbol,
                                    context,
                                    isExtension = receiverTypeReference != null,
                                    lazyDelegateExpression = lazyDelegateExpression,
                                    lazyBodyForGeneratedAccessors = lazyBody,
                                    ::bindFunctionTarget,
                                )
                            }
                        }
                    }
                    annotations += when {
                        isLocal -> propertyAnnotations
                        else -> propertyAnnotations.filterStandalonePropertyRelevantAnnotations(isVar)
                    }

                    contextReceivers.addAll(convertContextReceivers(this@toFirProperty.contextReceivers))
                }.also {
                    if (!isLocal) {
                        fillDanglingConstraintsTo(it)
                    }
                }
            }
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: FirElement?): FirElement {
            return buildAnonymousInitializer(initializer, containingDeclarationSymbol = null)
        }

        protected fun buildAnonymousInitializer(
            initializer: KtAnonymousInitializer,
            containingDeclarationSymbol: FirBasedSymbol<*>?,
            allowLazyBody: Boolean = true,
        ) = buildAnonymousInitializer {
            withContainerSymbol(symbol) {
                source = initializer.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                body = if (allowLazyBody) {
                    buildOrLazyBlock {
                        withForcedLocalContext {
                            initializer.body.toFirBlock()
                        }
                    }
                } else {
                    withForcedLocalContext {
                        initializer.body.toFirBlock()
                    }
                }

                this.containingDeclarationSymbol = containingDeclarationSymbol
                initializer.extractAnnotationsTo(this)
            }
        }

        override fun visitProperty(property: KtProperty, data: FirElement?): FirElement {
            return property.toFirProperty(
                ownerRegularOrAnonymousObjectSymbol = null,
                context = context
            )
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: FirElement?): FirElement {
            return typeReference.toFirType()
        }

        private fun KtTypeReference.toFirType(): FirTypeRef {
            val typeElement = typeElement
            val source = toFirSourceElement()
            val isNullable = typeElement is KtNullableType

            // There can be KtDeclarationModifierLists in the KtTypeReference AND the descendant KtNullableTypes.
            // We aggregate them to get modifiers and annotations. Not only that, there could be multiple modifier lists on each. Examples:
            //
            //   `@A() (@B Int)`   -> Has 2 modifier lists (@A and @B) on KtTypeReference
            //   `(@A() (@B Int))? -> No modifier list on KtTypeReference, but 2 modifier lists (@A and @B) on child KtNullableType
            //   `@A() (@B Int)?   -> Has 1 modifier list (@A) on KtTypeReference, and 1 modifier list (@B) on child KtNullableType
            //   `@A (@B() (@C() (@Bar D)?)?)?` -> Has 1 modifier list (@A) on KtTypeReference and 1 modifier list on each of the
            //                                     3 descendant KtNullableTypes (@B, @C, @D)
            //
            // We need to examine all modifier lists for some cases:
            // 1. `@A Int?` and `(@A Int)?` are effectively the same, but in the latter, the modifier list is on the child KtNullableType
            // 2. `(suspend @A () -> Int)?` is a nullable suspend function type but the modifier list is on the child KtNullableType
            //
            // `getModifierList()` only returns the first one so we have to get all modifier list children.
            // TODO: Report MODIFIER_LIST_NOT_ALLOWED error when there are multiple modifier lists. How do we report on each of them?
            fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
                getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)

            val allModifierLists = mutableListOf<KtModifierList>(*getAllModifierLists())

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                when (this) {
                    is KtNullableType -> {
                        allModifierLists += getAllModifierLists()
                        this.innerType.unwrapNullable()
                    }
                    else -> this
                }

            val firTypeBuilder = when (val unwrappedElement = typeElement.unwrapNullable()) {
                is KtDynamicType -> FirDynamicTypeRefBuilder().apply {
                    this.source = source
                    isMarkedNullable = isNullable
                }
                is KtUserType -> {
                    val referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        convertKtTypeElement(source, isNullable, unwrappedElement, referenceExpression)
                    } else {
                        FirErrorTypeRefBuilder().apply {
                            this.source = source
                            diagnostic = ConeSyntaxDiagnostic("Incomplete user type")

                            val qualifier = unwrappedElement.qualifier
                            val reference = qualifier?.referenceExpression
                            if (qualifier != null && reference != null) {
                                partiallyResolvedTypeRef = convertKtTypeElement(
                                    qualifier.toFirSourceElement(),
                                    isNullable = false,
                                    qualifier,
                                    reference
                                ).build()
                            }
                        }
                    }
                }
                is KtFunctionType -> {
                    FirFunctionTypeRefBuilder().apply {
                        this.source = source
                        isMarkedNullable = isNullable
                        isSuspend = allModifierLists.any { it.hasSuspendModifier() }
                        receiverTypeRef = unwrappedElement.receiverTypeReference?.toFirType()
                        // TODO: probably implicit type should not be here
                        returnTypeRef = unwrappedElement.returnTypeReference.toFirOrErrorType()
                        for (valueParameter in unwrappedElement.parameters) {
                            parameters += buildFunctionTypeParameter {
                                val parameterSource = valueParameter.toFirSourceElement()
                                this.source = parameterSource
                                name = valueParameter.nameAsName
                                returnTypeRef = when {
                                    valueParameter.typeReference != null -> valueParameter.typeReference.toFirOrErrorType()
                                    else -> createNoTypeForParameterTypeRef(parameterSource)
                                }
                            }
                        }

                        contextReceiverTypeRefs.addAll(
                            unwrappedElement.contextReceiversTypeReferences.mapNotNull {
                                it.toFirType()
                            }
                        )
                    }
                }
                is KtIntersectionType -> FirIntersectionTypeRefBuilder().apply {
                    this.source = source
                    isMarkedNullable = isNullable
                    leftType = unwrappedElement.getLeftTypeRef().toFirOrErrorType()
                    rightType = unwrappedElement.getRightTypeRef().toFirOrErrorType()
                }
                null -> FirErrorTypeRefBuilder().apply {
                    this.source = source
                    diagnostic = ConeSyntaxDiagnostic("Incomplete code")
                }
                else -> errorWithAttachment("Unexpected type element: ${unwrappedElement::class.simpleName}") {
                    withPsiEntry("unwrappedElement", unwrappedElement)
                }
            }

            for (modifierList in allModifierLists) {
                for (annotationEntry in modifierList.annotationEntries) {
                    firTypeBuilder.annotations += annotationEntry.convert<FirAnnotation>()
                }
            }

            return firTypeBuilder.build() as FirTypeRef
        }

        private fun convertKtTypeElement(
            source: KtPsiSourceElement,
            isNullable: Boolean,
            ktUserType: KtUserType,
            reference: KtSimpleNameExpression,
        ): FirUserTypeRefBuilder {
            var referenceExpression: KtSimpleNameExpression? = reference
            return FirUserTypeRefBuilder().apply {
                this.source = source
                isMarkedNullable = isNullable
                var ktQualifier: KtUserType? = ktUserType

                do {
                    val firQualifier = FirQualifierPartImpl(
                        referenceExpression!!.toFirSourceElement(),
                        referenceExpression!!.getReferencedNameAsName(),
                        FirTypeArgumentListImpl(ktQualifier?.typeArgumentList?.toKtPsiSourceElement() ?: source).apply {
                            typeArguments.appendTypeArguments(ktQualifier!!.typeArguments)
                        }
                    )
                    qualifier.add(firQualifier)

                    ktQualifier = ktQualifier!!.qualifier
                    referenceExpression = ktQualifier?.referenceExpression
                } while (referenceExpression != null)

                qualifier.reverse()
            }
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: FirElement?): FirElement {
            return buildAnnotationCall {
                source = annotationEntry.toFirSourceElement()
                useSiteTarget = annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()
                annotationTypeRef = annotationEntry.typeReference.toFirOrErrorType()
                annotationEntry.extractArgumentsTo(this)
                val name = (annotationTypeRef as? FirUserTypeRef)?.qualifier?.last()?.name ?: Name.special("<no-annotation-name>")
                calleeReference = buildSimpleNamedReference {
                    source = (annotationEntry.typeReference?.typeElement as? KtUserType)?.referenceExpression?.toFirSourceElement()
                    this.name = name
                }
                typeArguments.appendTypeArguments(annotationEntry.typeArguments)
                containingDeclarationSymbol = context.containerSymbol
            }
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: FirElement?): FirElement {
            val projectionKind = typeProjection.projectionKind
            val projectionSource = typeProjection.toFirSourceElement()
            if (projectionKind == KtProjectionKind.STAR) {
                return buildStarProjection {
                    source = projectionSource
                }
            }
            val argumentList = typeProjection.parent as? KtTypeArgumentList
            val typeReference = typeProjection.typeReference
            if (argumentList?.parent is KtCallExpression && typeReference?.isPlaceholder == true) {
                return buildPlaceholderProjection {
                    source = projectionSource
                }
            }
            val firType = typeReference.toFirOrErrorType()
            return buildTypeProjectionWithVariance {
                source = projectionSource
                typeRef = firType
                variance = when (projectionKind) {
                    KtProjectionKind.IN -> Variance.IN_VARIANCE
                    KtProjectionKind.OUT -> Variance.OUT_VARIANCE
                    KtProjectionKind.NONE -> Variance.INVARIANT
                    KtProjectionKind.STAR -> shouldNotBeCalled()
                }
            }
        }

        override fun visitBlockExpression(expression: KtBlockExpression, data: FirElement?): FirElement {
            return configureBlockWithoutBuilding(expression).build()
        }

        private fun configureBlockWithoutBuilding(expression: KtBlockExpression, kind: KtFakeSourceElementKind? = null): FirBlockBuilder {
            return FirBlockBuilder().apply {
                source = expression.toFirSourceElement(kind)
                for (statement in expression.statements) {
                    val firStatement = statement.toFirStatement { "Statement expected: ${statement.text}" }
                    val isForLoopBlock =
                        firStatement is FirBlock && firStatement.source?.kind == KtFakeSourceElementKind.DesugaredForLoop
                    if (firStatement !is FirBlock || isForLoopBlock || firStatement.annotations.isNotEmpty()) {
                        statements += firStatement
                    } else {
                        statements += firStatement.statements
                    }
                }
            }
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: FirElement?): FirElement {
            val qualifiedSource = when {
                expression.getQualifiedExpressionForSelector() != null -> expression.parent
                else -> expression
            }.toFirSourceElement()

            val expressionSource = expression.toFirSourceElement()
            var diagnostic: ConeDiagnostic? = null
            val rawText = expression.getReferencedNameElement().node.text
            if (rawText.isUnderscore) {
                diagnostic = ConeUnderscoreUsageWithoutBackticks(expressionSource)
            }

            return generateAccessExpression(
                qualifiedSource,
                expressionSource,
                expression.getReferencedNameAsName(),
                diagnostic
            )
        }

        override fun visitConstantExpression(expression: KtConstantExpression, data: FirElement?): FirElement =
            generateConstantExpressionByLiteral(expression)

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: FirElement?): FirElement {
            return expression.entries.toInterpolatingCall(
                expression,
                getElementType = { element ->
                    when (element) {
                        is KtLiteralStringTemplateEntry -> KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY
                        is KtEscapeStringTemplateEntry -> KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY
                        is KtSimpleNameStringTemplateEntry -> KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY
                        is KtBlockStringTemplateEntry -> KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY
                        else -> errorWithAttachment("invalid node type ${element::class}") {
                            withPsiEntry("element", element)
                        }
                    }
                },
                convertTemplateEntry = {
                    (this as KtStringTemplateEntryWithExpression).expression.toFirExpression(it)
                },
            )
        }

        override fun visitReturnExpression(expression: KtReturnExpression, data: FirElement?): FirElement {
            val source = expression.toFirSourceElement(KtFakeSourceElementKind.ImplicitUnit.Return)
            val result = expression.returnedExpression?.toFirExpression("Incorrect return expression")
                ?: buildUnitExpression { this.source = source }
            return result.toReturn(source, expression.getTargetLabel()?.getReferencedName(), fromKtReturnExpression = true)
        }

        override fun visitTryExpression(expression: KtTryExpression, data: FirElement?): FirElement {
            return buildTryExpression {
                source = expression.toFirSourceElement()
                tryBlock = expression.tryBlock.toFirBlock()
                finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.let { ktParameter ->
                        val name = convertValueParameterName(
                            ktParameter.nameAsSafeName,
                            ValueParameterDeclaration.CATCH
                        ) { ktParameter.nameIdentifier?.node?.text }

                        buildProperty {
                            val parameterSource = ktParameter.toFirSourceElement()
                            source = parameterSource
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = when {
                                ktParameter.typeReference != null -> ktParameter.typeReference.toFirOrErrorType()
                                else -> createNoTypeForParameterTypeRef(parameterSource)
                            }
                            isVar = false
                            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                            isLocal = true
                            this.name = name
                            symbol = FirPropertySymbol(CallableId(name))
                            for (annotationEntry in ktParameter.annotationEntries) {
                                this.annotations += annotationEntry.convert<FirAnnotation>()
                            }
                        }.also {
                            it.isCatchParameter = true
                        }
                    } ?: continue
                    catches += buildCatch {
                        source = clause.toFirSourceElement()
                        this.parameter = parameter
                        block = clause.catchBody.toFirBlock()
                    }
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression, data: FirElement?): FirElement {
            return buildWhenExpression {
                source = expression.toFirSourceElement()

                var ktLastIf: KtIfExpression = expression
                whenBranches@ while (true) {
                    val ktCondition = ktLastIf.condition
                    branches += buildWhenBranch {
                        source = ktCondition?.toFirSourceElement(KtFakeSourceElementKind.WhenCondition)
                        condition = ktCondition.toFirExpression("If statement should have condition")
                        result = ktLastIf.then.toFirBlock()
                    }

                    when (val ktElse = ktLastIf.`else`) {
                        null -> break@whenBranches
                        is KtIfExpression -> ktLastIf = ktElse
                        else -> {
                            branches += buildWhenBranch {
                                source = ktLastIf.elseKeyword?.toKtPsiSourceElement()
                                condition = buildElseIfTrueCondition()
                                result = ktLastIf.`else`.toFirBlock()
                            }
                            break@whenBranches
                        }
                    }
                }

                usedAsExpression = expression.usedAsExpression
            }
        }

        override fun visitWhenExpression(expression: KtWhenExpression, data: FirElement?): FirElement {
            val ktSubjectExpression = expression.subjectExpression
            val subjectExpression = when (ktSubjectExpression) {
                is KtVariableDeclaration -> ktSubjectExpression.initializer
                else -> ktSubjectExpression
            }?.toFirExpression("Incorrect when subject expression: ${ktSubjectExpression?.text}")
            val subjectVariable = when (ktSubjectExpression) {
                is KtVariableDeclaration -> {
                    val name = ktSubjectExpression.nameAsSafeName
                    buildProperty {
                        source = ktSubjectExpression.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = ktSubjectExpression.typeReference.toFirOrImplicitType()
                        this.name = name
                        initializer = subjectExpression
                        delegate = null
                        isVar = false
                        symbol = FirPropertySymbol(name)
                        isLocal = true
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                        ktSubjectExpression.extractAnnotationsTo(this)
                    }
                }
                else -> null
            }
            val hasSubject = subjectExpression != null

            @OptIn(FirContractViolation::class)
            val ref = FirExpressionRef<FirWhenExpression>()
            var shouldBind = hasSubject
            return buildWhenExpression {
                source = expression.toFirSourceElement()
                this.subject = subjectExpression
                this.subjectVariable = subjectVariable
                usedAsExpression = expression.usedAsExpression

                for (entry in expression.entries) {
                    val entrySource = entry.toFirSourceElement()
                    val branchBody = entry.expression.toFirBlock()
                    branches += if (!entry.isElse) {
                        if (hasSubject) {
                            buildWhenBranch {
                                source = entrySource
                                condition = entry.conditions.toFirWhenCondition(
                                    ref,
                                    { toFirExpression(it) },
                                    { toFirOrErrorType() },
                                )
                                result = branchBody
                            }
                        } else {
                            val ktCondition = entry.conditions.first()
                            buildWhenBranch {
                                source = entrySource
                                condition =
                                    if (entry.conditions.size == 1 && ktCondition is KtWhenConditionWithExpression) {
                                        (ktCondition.expression ?: ktCondition).toFirExpression(
                                            "No expression in condition with expression",
                                            DiagnosticKind.ExpressionExpected,
                                        )
                                    } else {
                                        buildBalancedOrExpressionTree(entry.conditions.map { condition ->
                                            if (condition is KtWhenConditionWithExpression) {
                                                condition.expression.toFirExpression(
                                                    "No expression in condition with expression",
                                                    DiagnosticKind.ExpressionExpected
                                                )
                                            } else {
                                                shouldBind = true
                                                buildErrorExpression {
                                                    source = condition.toFirSourceElement()
                                                    nonExpressionElement = condition.toFirWhenCondition(
                                                        ref,
                                                        { toFirExpression(it) },
                                                        { toFirOrErrorType() },
                                                    )
                                                    diagnostic = ConeSimpleDiagnostic(
                                                        "No expression in condition with expression",
                                                        DiagnosticKind.ExpressionExpected,
                                                    )
                                                }
                                            }
                                        })
                                    }
                                result = branchBody
                            }
                        }
                    } else {
                        buildWhenBranch {
                            source = entrySource
                            condition = buildElseIfTrueCondition()
                            result = branchBody
                        }
                    }
                }
            }.also {
                if (shouldBind) {
                    ref.bind(it)
                }
            }
        }

        private val KtExpression.usedAsExpression: Boolean
            get() {
                var parent = parent
                while (parent.elementType == KtNodeTypes.ANNOTATED_EXPRESSION ||
                    parent.elementType == KtNodeTypes.LABELED_EXPRESSION
                ) {
                    parent = parent.parent
                }
                if (parent is KtBlockExpression) return false
                when (parent.elementType) {
                    KtNodeTypes.THEN, KtNodeTypes.ELSE, KtNodeTypes.WHEN_ENTRY -> {
                        return (parent.parent as? KtExpression)?.usedAsExpression != false
                    }
                }
                if (parent is KtScriptInitializer) return false
                // Here we check that when used is a single statement of a loop
                if (parent !is KtContainerNodeForControlStructureBody) return true
                val type = parent.parent.elementType
                return !(type == KtNodeTypes.FOR || type == KtNodeTypes.WHILE || type == KtNodeTypes.DO_WHILE)
            }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: FirElement?): FirElement {
            val target: FirLoopTarget
            return FirDoWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
                target = prepareTarget(expression)
                condition = expression.condition.toFirExpression("No condition in do-while loop")
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: FirElement?): FirElement {
            val target: FirLoopTarget
            return FirWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                condition = expression.condition.toFirExpression("No condition in while loop")
                // break/continue in the while loop condition will refer to an outer loop if any.
                // So, prepare the loop target after building the condition.
                target = prepareTarget(expression)
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: FirElement?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val ktParameter = expression.loopParameter
            val fakeSource = expression.toKtPsiSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
            val rangeSource = expression.loopRange?.toFirSourceElement(KtFakeSourceElementKind.DesugaredForLoop) ?: fakeSource

            val target: FirLoopTarget
            // NB: FirForLoopChecker relies on this block existence and structure
            return buildBlock {
                source = fakeSource
                val iteratorVal = generateTemporaryVariable(
                    baseModuleData, rangeSource, SpecialNames.ITERATOR,
                    buildFunctionCall {
                        source = rangeSource
                        calleeReference = buildSimpleNamedReference {
                            source = rangeSource
                            name = OperatorNameConventions.ITERATOR
                        }
                        explicitReceiver = rangeExpression
                        origin = FirFunctionCallOrigin.Operator
                    },
                )
                statements += iteratorVal
                statements += FirWhileLoopBuilder().apply {
                    source = expression.toFirSourceElement()
                    condition = buildFunctionCall {
                        source = rangeSource
                        calleeReference = buildSimpleNamedReference {
                            source = rangeSource
                            name = OperatorNameConventions.HAS_NEXT
                        }
                        explicitReceiver = generateResolvedAccessExpression(rangeSource, iteratorVal)
                        origin = FirFunctionCallOrigin.Operator
                    }
                    // break/continue in the for loop condition will refer to an outer loop if any.
                    // So, prepare the loop target after building the condition.
                    target = prepareTarget(expression)
                }.configure(target) {
                    val blockBuilder = FirBlockBuilder().apply {
                        source = expression.toFirSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
                    }
                    if (ktParameter != null) {
                        val multiDeclaration = ktParameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            moduleData = baseModuleData,
                            source = expression.loopParameter?.toFirSourceElement(),
                            name = if (multiDeclaration != null) SpecialNames.DESTRUCT else ktParameter.nameAsSafeName,
                            initializer = buildFunctionCall {
                                source = rangeSource
                                calleeReference = buildSimpleNamedReference {
                                    source = rangeSource
                                    name = OperatorNameConventions.NEXT
                                }
                                explicitReceiver = generateResolvedAccessExpression(rangeSource, iteratorVal)
                                origin = FirFunctionCallOrigin.Operator
                            },
                            typeRef = ktParameter.typeReference.toFirOrImplicitType(),
                            extractedAnnotations = ktParameter.modifierList?.annotationEntries?.map { it.convert<FirAnnotation>() },
                        )
                        if (multiDeclaration != null) {
                            blockBuilder.statements.addDestructuringVariables(
                                baseModuleData,
                                multiDeclaration = multiDeclaration,
                                container = firLoopParameter,
                                tmpVariable = true,
                                localEntries = true,
                            )
                        } else {
                            blockBuilder.statements.add(firLoopParameter)
                        }
                    }
                    blockBuilder.statements.add(expression.body.toFirBlock())
                    blockBuilder.build()
                }
            }
        }

        override fun visitBreakExpression(expression: KtBreakExpression, data: FirElement?): FirElement {
            return FirBreakExpressionBuilder().apply {
                source = expression.toFirSourceElement()
            }.bindLabel(expression).build()
        }

        override fun visitContinueExpression(expression: KtContinueExpression, data: FirElement?): FirElement {
            return FirContinueExpressionBuilder().apply {
                source = expression.toFirSourceElement()
            }.bindLabel(expression).build()
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression, data: FirElement?): FirElement {
            val operationToken = expression.operationToken

            if (operationToken == IDENTIFIER) {
                context.calleeNamesForLambda += expression.operationReference.getReferencedNameAsName()
            } else {
                context.calleeNamesForLambda += null
            }

            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")

            // No need for the callee name since arguments are already generated
            context.calleeNamesForLambda.removeLast()

            val source = expression.toFirSourceElement()

            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(rightArgument, source)
                ANDAND, OROR ->
                    return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND, source)
                in OperatorConventions.IN_OPERATIONS ->
                    return rightArgument.generateContainsOperation(
                        leftArgument, operationToken == NOT_IN, source,
                        expression.operationReference.toFirSourceElement(),
                    )
                in OperatorConventions.COMPARISON_OPERATIONS ->
                    return leftArgument.generateComparisonExpression(
                        rightArgument, operationToken, source,
                        expression.operationReference.toFirSourceElement(),
                    )
            }
            val conventionCallName = operationToken.toBinaryName()
            return if (conventionCallName != null || operationToken == IDENTIFIER) {
                buildFunctionCall {
                    this.source = source
                    calleeReference = buildSimpleNamedReference {
                        this.source = expression.operationReference.toFirSourceElement()
                        name = conventionCallName ?: expression.operationReference.getReferencedNameAsName()
                    }
                    explicitReceiver = leftArgument
                    argumentList = buildUnaryArgumentList(rightArgument)
                    origin = if (conventionCallName != null) FirFunctionCallOrigin.Operator else FirFunctionCallOrigin.Infix
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                if (firOperation in FirOperation.ASSIGNMENTS) {
                    return expression.left.generateAssignment(
                        source,
                        expression.left?.toFirSourceElement(),
                        rightArgument,
                        firOperation,
                        leftArgument.annotations,
                        expression.right,
                    ) {
                        (this as KtExpression).toFirExpression(
                            sourceWhenInvalidExpression = expression,
                            isValidExpression = { !it.isStatementLikeExpression || it.isArraySet },
                        ) {
                            ConeSimpleDiagnostic(
                                "Incorrect expression in assignment: ${expression.text}",
                                DiagnosticKind.ExpressionExpected
                            )
                        }
                    }
                } else {
                    buildEqualityOperatorCall {
                        this.source = source
                        operation = firOperation
                        argumentList = buildBinaryArgumentList(leftArgument, rightArgument)
                    }
                }
            }
        }

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: FirElement?): FirElement {
            return buildTypeOperatorCall {
                source = expression.toFirSourceElement()
                operation = expression.operationReference.getReferencedNameElementType().toFirOperation()
                conversionTypeRef = expression.right.toFirOrErrorType()
                argumentList = buildUnaryArgumentList(expression.left.toFirExpression("No left operand"))
            }
        }

        override fun visitIsExpression(expression: KtIsExpression, data: FirElement?): FirElement {
            return buildTypeOperatorCall {
                source = expression.toFirSourceElement()
                operation = if (expression.isNegated) FirOperation.NOT_IS else FirOperation.IS
                conversionTypeRef = expression.typeReference.toFirOrErrorType()
                argumentList = buildUnaryArgumentList(expression.leftHandSide.toFirExpression("No left operand"))
            }
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression, data: FirElement?): FirElement {
            val operationToken = expression.operationToken
            val argument = expression.baseExpression
            val conventionCallName = operationToken.toUnaryName()
            return when {
                operationToken == EXCLEXCL -> {
                    buildCheckNotNullCall {
                        source = expression.toFirSourceElement()
                        argumentList = buildUnaryArgumentList(argument.toFirExpression("No operand"))
                    }
                }
                conventionCallName != null -> {
                    if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                        return generateIncrementOrDecrementBlock(
                            expression, expression.operationReference, argument,
                            callName = conventionCallName,
                            prefix = expression is KtPrefixExpression,
                        ) { (this as KtExpression).toFirExpression("Incorrect expression inside inc/dec") }
                    }

                    val receiver = argument.toFirExpression("No operand")

                    convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary(expression, receiver, operationToken)?.let { return it }

                    buildFunctionCall {
                        source = expression.toFirSourceElement()
                        calleeReference = buildSimpleNamedReference {
                            source = expression.operationReference.toFirSourceElement()
                            name = conventionCallName
                        }
                        explicitReceiver = receiver
                        origin = FirFunctionCallOrigin.Operator
                    }
                }
                else -> throw IllegalStateException("Unexpected expression: ${expression.text}")
            }
        }

        private fun splitToCalleeAndReceiver(
            calleeExpression: KtExpression?,
            defaultSource: KtPsiSourceElement,
        ): CalleeAndReceiver {
            val parenthesizedArgument = (calleeExpression as? KtParenthesizedExpression)?.expression
                ?.toFirExpression("Incorrect invoke receiver")

            return when {
                calleeExpression is KtSimpleNameExpression ->
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = calleeExpression.toFirSourceElement()
                            name = calleeExpression.getReferencedNameAsName()
                        }
                    )

                calleeExpression is KtSuperExpression || (parenthesizedArgument as? FirResolvable)?.calleeReference is FirSuperReference -> {
                    CalleeAndReceiver(
                        buildErrorNamedReference {
                            source = (calleeExpression as? KtSuperExpression)?.toFirSourceElement()
                                ?: (parenthesizedArgument as? FirResolvable)?.calleeReference?.source
                            diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed)
                        }
                    )
                }

                parenthesizedArgument != null -> {
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = defaultSource.fakeElement(KtFakeSourceElementKind.ImplicitInvokeCall)
                            name = OperatorNameConventions.INVOKE
                        },
                        receiverExpression = parenthesizedArgument,
                        isImplicitInvoke = true
                    )
                }

                calleeExpression == null -> {
                    CalleeAndReceiver(
                        buildErrorNamedReference {
                            source = defaultSource
                            diagnostic = ConeSyntaxDiagnostic("Call has no callee")
                        }
                    )
                }

                else -> {
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = defaultSource.fakeElement(KtFakeSourceElementKind.ImplicitInvokeCall)
                            name = OperatorNameConventions.INVOKE
                        },
                        receiverExpression = calleeExpression.toFirExpression("Incorrect invoke receiver"),
                        isImplicitInvoke = true
                    )
                }
            }
        }

        override fun visitCallExpression(expression: KtCallExpression, data: FirElement?): FirElement {
            val source = expression.toFirSourceElement()
            val (calleeReference, explicitReceiver, isImplicitInvoke) = splitToCalleeAndReceiver(expression.calleeExpression, source)

            val result: FirQualifiedAccessExpressionBuilder =
                if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
                    FirPropertyAccessExpressionBuilder().apply {
                        this.source = source
                        this.calleeReference = calleeReference
                    }
                } else {
                    val builder = if (isImplicitInvoke) FirImplicitInvokeCallBuilder() else FirFunctionCallBuilder()
                    builder.apply {
                        this.source = source
                        this.calleeReference = calleeReference
                        context.calleeNamesForLambda += calleeReference.name
                        expression.extractArgumentsTo(this)
                        context.calleeNamesForLambda.removeLast()
                    }
                }

            return result.apply {
                this.explicitReceiver = explicitReceiver
                typeArguments.appendTypeArguments(expression.typeArguments)
            }.build()
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: FirElement?): FirElement {
            val arrayExpression = expression.arrayExpression
            val setArgument = context.arraySetArgument.remove(expression)
            return buildFunctionCall {
                val isGet = setArgument == null
                source = (if (isGet) expression else expression.parent).toFirSourceElement()
                calleeReference = buildSimpleNamedReference {
                    source = expression.toFirSourceElement().fakeElement(KtFakeSourceElementKind.ArrayAccessNameReference)
                    name = if (isGet) OperatorNameConventions.GET else OperatorNameConventions.SET
                }
                explicitReceiver = arrayExpression.toFirExpression("No array expression")
                argumentList = buildArgumentList {
                    for (indexExpression in expression.indexExpressions) {
                        arguments += indexExpression.toFirExpression("Incorrect index expression")
                    }
                    if (setArgument != null) {
                        arguments += setArgument
                    }
                }
                origin = FirFunctionCallOrigin.Operator
            }.pullUpSafeCallIfNecessary()
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: FirElement?): FirElement {
            val receiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")

            val selector = expression.selectorExpression
                ?: return buildErrorExpression {
                    source = expression.toFirSourceElement()
                    diagnostic = ConeSyntaxDiagnostic("Qualified expression without selector")

                    // if there is no selector, we still want to resolve the receiver
                    this.expression = receiver
                }

            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirQualifiedAccessExpression) {
                if (expression is KtSafeQualifiedExpression) {
                    @OptIn(FirImplementationDetail::class)
                    firSelector.replaceSource(expression.toFirSourceElement(KtFakeSourceElementKind.DesugaredSafeCallExpression))
                    return firSelector.createSafeCall(
                        receiver,
                        expression.toFirSourceElement()
                    )
                }

                return convertFirSelector(firSelector, expression.toFirSourceElement(), receiver)
            }
            if (firSelector is FirErrorExpression) {
                return buildQualifiedErrorAccessExpression {
                    this.receiver = receiver
                    this.selector = firSelector
                    source = expression.toFirSourceElement()
                    diagnostic = ConeSyntaxDiagnostic("Qualified expression with unexpected selector")
                }
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: FirElement?): FirElement {
            return buildThisReceiverExpression {
                val sourceElement = expression.toFirSourceElement()
                source = sourceElement
                calleeReference = buildExplicitThisReference {
                    source = sourceElement.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                    labelName = expression.getLabelName()
                }
            }
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: FirElement?): FirElement {
            val superType = expression.superTypeQualifier
            val theSource = expression.toFirSourceElement()
            return buildPropertyAccessExpression {
                this.source = theSource
                calleeReference = buildExplicitSuperReference {
                    source = theSource.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                    labelName = expression.getLabelName()
                    superTypeRef = superType.toFirOrImplicitType()
                }
            }
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: FirElement?): FirElement {
            context.forwardLabelUsagePermission(expression, expression.expression)
            return expression.expression.toFirExpression("Empty parentheses")
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: FirElement?): FirElement {
            val label = expression.getTargetLabel()
            var labelSource: KtSourceElement? = null
            var forbiddenLabelKind: ForbiddenLabelKind? = null

            val isRepetitiveLabel = expression.baseExpression is KtLabeledExpression

            val result = if (label != null) {
                val name = label.getReferencedNameElement().node!!.text
                forbiddenLabelKind = getForbiddenLabelKind(name, isRepetitiveLabel)
                labelSource = label.toKtPsiSourceElement()
                val firLabel = buildLabel(name, labelSource)
                context.withNewLabel(firLabel, expression.baseExpression) {
                    expression.baseExpression?.accept(this, data)
                }
            } else {
                expression.baseExpression?.accept(this, data)
            }

            return buildExpressionHandlingErrors(result, expression.toFirSourceElement(), forbiddenLabelKind, labelSource)
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: FirElement?): FirElement {
            val baseExpression = expression.baseExpression
            context.forwardLabelUsagePermission(expression, baseExpression)
            val rawResult = baseExpression?.accept(this, data)
            val result = rawResult as? FirAnnotationContainer
                ?: buildErrorExpression(
                    expression.toFirSourceElement(),
                    ConeNotAnnotationContainer(rawResult?.render() ?: "???")
                )
            expression.extractAnnotationsTo(result)
            return result
        }

        override fun visitThrowExpression(expression: KtThrowExpression, data: FirElement?): FirElement {
            return buildThrowExpression {
                source = expression.toFirSourceElement()
                exception = expression.thrownExpression.toFirExpression("Nothing to throw")
            }
        }

        override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: FirElement?): FirElement {
            val baseVariable = generateTemporaryVariable(
                baseModuleData,
                multiDeclaration.toFirSourceElement(),
                "destruct",
                multiDeclaration.initializer.toFirExpression { ConeSyntaxDiagnostic("Initializer required for destructuring declaration") },
                extractAnnotationsTo = { extractAnnotationsTo(it) }
            )
            return generateDestructuringBlock(
                baseModuleData,
                multiDeclaration,
                baseVariable,
                tmpVariable = true,
                localEntries = true,
            )
        }

        override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: FirElement?): FirElement {
            return buildGetClassCall {
                source = expression.toFirSourceElement()
                argumentList = buildUnaryArgumentList(
                    expression.receiverExpression.toFirExpression(sourceWhenThisIsNull = expression) {
                        ConeUnsupportedClassLiteralsWithEmptyLhs
                    }
                )
            }
        }

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: FirElement?): FirElement {
            return buildCallableReferenceAccess {
                source = expression.toFirSourceElement()
                calleeReference = buildSimpleNamedReference {
                    source = expression.callableReference.toFirSourceElement()
                    name = expression.callableReference.getReferencedNameAsName()
                }
                explicitReceiver = expression.receiverExpression?.toFirExpression("Incorrect receiver expression")
                hasQuestionMarkAtLHS = expression.hasQuestionMarks
            }
        }

        override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: FirElement?): FirElement {
            return buildArrayLiteral {
                source = expression.toFirSourceElement()
                argumentList = buildArgumentList {
                    for (innerExpression in expression.getInnerExpressions()) {
                        arguments += innerExpression.toFirExpression("Incorrect collection literal argument")
                    }
                }
            }
        }

        override fun visitExpression(expression: KtExpression, data: FirElement?): FirElement {
            return buildExpressionStub {
                source = expression.toFirSourceElement()
            }
        }

        private fun MutableList<FirTypeProjection>.appendTypeArguments(args: List<KtTypeProjection>) {
            for (typeArgument in args) {
                this += typeArgument.convert<FirTypeProjection>()
            }
        }

        private fun buildErrorTopLevelDeclarationForDanglingModifierList(modifierList: KtModifierList) = buildDanglingModifierList {
            this.source = modifierList.toFirSourceElement(KtFakeSourceElementKind.DanglingModifierList)
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            diagnostic = ConeDanglingModifierOnTopLevel
            symbol = FirDanglingModifierSymbol()
            withContainerSymbol(symbol) {
                for (annotationEntry in modifierList.getAnnotationEntries()) {
                    annotations += annotationEntry.convert<FirAnnotation>()
                }
            }
        }
    }
}

enum class BodyBuildingMode {
    /**
     * Build every expression and every body
     */
    NORMAL,

    /**
     * Build [FirLazyBlock] for function bodies, constructors & getters/setters
     * Build [FirLazyExpression] for property initializers
     */
    LAZY_BODIES;

    companion object {
        fun lazyBodies(lazyBodies: Boolean): BodyBuildingMode = if (lazyBodies) LAZY_BODIES else NORMAL
    }
}
