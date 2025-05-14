/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.AstLoadingFilter
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.firstFunctionCallInBlockHasLambdaArgumentWithLabel
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
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.buildErrorNamedReferenceWithNoName
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
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

    fun buildTypeReference(reference: KtTypeReference): FirTypeRef {
        return reference.accept(Visitor(), null) as FirTypeRef
    }

    override fun PsiElement.toFirSourceElement(kind: KtFakeSourceElementKind?): KtPsiSourceElement {
        val actualKind = kind ?: KtRealSourceElementKind
        return this.toKtPsiSourceElement(actualKind)
    }

    override val PsiElement.elementType: IElementType
        get() {
            val stubBasedElement = this as? StubBasedPsiElementBase<*>
            return stubBasedElement?.elementType ?: node.elementType
        }

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

    private val KtConstructor<*>.constructorExplicitVisibility: Visibility?
        get() = getVisibility().takeUnless { it == Visibilities.Unknown }

    // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
    private fun constructorDefaultVisibility(owner: KtClassOrObject): Visibility = when {
        owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) || owner is KtEnumEntry -> Visibilities.Private
        owner.hasModifier(SEALED_KEYWORD) -> Visibilities.Protected
        else -> Visibilities.Unknown
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
            forceLocal: Boolean = false,
        ): FirProperty = property.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol,
            context,
            forceLocal
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
        private fun (() -> KtExpression?).toFirExpression(errorReason: String, sourceWhenInvalidExpression: KtElement): FirExpression =
            this().toFirExpression(errorReason, sourceWhenInvalidExpression = sourceWhenInvalidExpression)

        private fun KtElement?.toFirExpression(
            errorReason: String,
            sourceWhenInvalidExpression: KtElement,
        ): FirExpression {
            return toFirExpression(sourceWhenInvalidExpression = sourceWhenInvalidExpression) { missing ->
                if (missing) {
                    ConeSyntaxDiagnostic(errorReason)
                } else {
                    ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
                }
            }
        }

        private fun KtElement.toFirExpression(
            errorReason: String,
        ): FirExpression {
            return toFirExpression(sourceWhenInvalidExpression = this) { missing ->
                if (missing) {
                    ConeSyntaxDiagnostic(errorReason)
                } else {
                    ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected)
                }
            }
        }

        private inline fun KtElement?.toFirExpression(
            sourceWhenInvalidExpression: KtElement,
            isValidExpression: (FirExpression) -> Boolean = { !it.isStatementLikeExpression },
            diagnosticFn: (missing: Boolean) -> ConeDiagnostic,
        ): FirExpression {
            if (this == null) {
                return buildErrorExpression(source = sourceWhenInvalidExpression.toFirSourceElement(), diagnosticFn(true))
            }

            return when (val fir = convertElement(this, null)) {
                is FirExpression -> when {
                    isValidExpression(fir) -> checkSelectorInvariant(fir)
                    else -> buildErrorExpression {
                        nonExpressionElement = fir
                        diagnostic = diagnosticFn(false)
                        source = fir.source?.realElement() ?: sourceWhenInvalidExpression.toFirSourceElement()
                    }
                }
                else -> buildErrorExpression {
                    nonExpressionElement = fir
                    diagnostic = diagnosticFn(fir == null)
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
                    val initializer = toInitializerExpression()
                    buildErrorNonLocalDestructuringDeclaration(toFirSourceElement(), initializer)
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
                                    functionCallHasLabel(blockSourcePsi) -> ConeContractMayNotHaveLabel
                                    else -> null
                                }
                                processLegacyContractDescription(block, diagnostic)
                            }
                            else -> null
                        }
                        return@buildFirBody block to contractDescription
                    } else {
                        val result = { bodyExpression }.toFirExpression("Function has no body (but should)", this)
                        FirSingleExpressionBlock(result.toReturn(baseSource = result.source))
                    }
                } to null
            } else {
                null to null
            }

        private fun isCallTheFirstStatement(psi: PsiElement): Boolean =
            isCallTheFirstStatement(psi, { it.elementType }, { it.allChildren.toList() })

        private fun functionCallHasLabel(psi: PsiElement): Boolean =
            firstFunctionCallInBlockHasLambdaArgumentWithLabel(psi, { it.elementType }, { it.allChildren.toList() })

        private fun ValueArgument.toFirExpression(): FirExpression {
            val name = this.getArgumentName()?.asName
            val firExpression = when (val expression = this.getArgumentExpression()) {
                is KtConstantExpression, is KtStringTemplateExpression -> {
                    expression.accept(this@Visitor, null) as FirExpression
                }

                else -> {
                    { expression }.toFirExpression("Argument is absent", sourceWhenInvalidExpression = this.asElement())
                }
            }

            val isSpread = isSpread
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
            // Downward propagation of `inline`, `external` and `expect` modifiers (from property to its accessors)
            val status =
                FirDeclarationStatusImpl(accessorVisibility, this?.modality).apply {
                    isInline = property.hasModifier(INLINE_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(INLINE_KEYWORD) == true
                    isExternal = property.hasModifier(EXTERNAL_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(EXTERNAL_KEYWORD) == true
                    isExpect = property.hasModifier(EXPECT_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(EXPECT_KEYWORD) == true
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

                this != null || isGetter || property.isVar -> {
                    // Default getter for val/var properties, default setter for var properties,
                    // and a default setter without body for val properties.
                    val propertySource =
                        this?.toFirSourceElement() ?: property.toKtPsiSourceElement(KtFakeSourceElementKind.DefaultAccessor)
                    val valueParameter = this?.valueParameters?.firstOrNull()

                    FirDefaultPropertyAccessor
                        .createGetterOrSetter(
                            propertySource,
                            baseModuleData,
                            FirDeclarationOrigin.Source,
                            propertyTypeRefToUse,
                            accessorVisibility,
                            propertySymbol,
                            isGetter,
                            parameterAnnotations = parameterAnnotationsFromProperty,
                            parameterSource = valueParameter?.toKtPsiSourceElement(),
                        )
                        .also {
                            if (this != null) {
                                it.extractAnnotationsFrom(this)
                            }
                            it.replaceAnnotations(it.annotations.smartPlus(accessorAnnotationsFromProperty))
                            it.status = status
                            it.initContainingClassAttr()

                            valueParameter?.typeReference?.toFirType()?.let { type ->
                                it.valueParameters.firstOrNull()?.replaceReturnTypeRef(type)
                            }
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
                isStatic = property.hasModifier(STATIC_KEYWORD) ||
                        declaration?.hasModifier(STATIC_KEYWORD) == true
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
            containingDeclarationSymbol: FirBasedSymbol<*>,
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
                withContainerSymbol(symbol, isLocal = !valueParameterDeclaration.isAnnotationOwner) {
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
                    if (valueParameterDeclaration == ValueParameterDeclaration.CONTEXT_PARAMETER) {
                        buildErrorExpression {
                            source = this@toFirValueParameter.toFirSourceElement(KtFakeSourceElementKind.ContextParameterDefaultValue)
                            diagnostic = ConeContextParameterWithDefaultValue
                        }
                    } else {
                        buildOrLazyExpression(null) {
                            { this@toFirValueParameter.defaultValue }.toFirExpression(
                                "Should have default value",
                                sourceWhenInvalidExpression = this@toFirValueParameter
                            )
                        }
                    }
                } else null
                isCrossinline = hasModifier(CROSSINLINE_KEYWORD)
                isNoinline = hasModifier(NOINLINE_KEYWORD)
                valueParameterKind = if (valueParameterDeclaration == ValueParameterDeclaration.CONTEXT_PARAMETER) {
                    FirValueParameterKind.ContextParameter
                } else {
                    FirValueParameterKind.Regular
                }
                this.containingDeclarationSymbol = containingDeclarationSymbol
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
                    parameterAnnotations += annotationEntry.convert<FirAnnotationCall>().let {
                        // Filter error annotation calls to avoid double-reporting of INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION
                        // (it's already reported on a value parameter)
                        // It also duplicates LT behavior, see ValueParameter.toFirPropertyFromPrimaryConstructor
                        if (it !is FirErrorAnnotationCall) it else buildAnnotationCallCopy(it) {}
                    }
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
                        source = defaultAccessorSource,
                        moduleData = baseModuleData,
                        origin = FirDeclarationOrigin.Source,
                        propertyTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                        visibility = status.visibility,
                        propertySymbol = symbol,
                        modality = status.modality,
                        isInline = hasModifier(INLINE_KEYWORD),
                    ).also { getter ->
                        getter.initContainingClassAttr()
                        getter.replaceAnnotations(parameterAnnotations.filterUseSiteTarget(PROPERTY_GETTER))
                    }
                    setter = if (isMutable) FirDefaultPropertySetter(
                        source = defaultAccessorSource,
                        moduleData = baseModuleData,
                        origin = FirDeclarationOrigin.Source,
                        propertyTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                        visibility = status.visibility,
                        propertySymbol = symbol,
                        modality = status.modality,
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

        private fun KtTypeParameterListOwner.convertTypeParameters(declarationSymbol: FirBasedSymbol<*>): MutableList<FirTypeParameterRef> {
            return typeParameters.mapTo(mutableListOf()) { typeParameter ->
                extractTypeParameter(typeParameter, declarationSymbol)
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
                                "Type parameter annotations are not allowed inside where clauses",
                                DiagnosticKind.AnnotationInWhereClause,
                            )
                            val name = (annotationTypeRef as? FirUserTypeRef)?.qualifier?.last()?.name
                                ?: Name.special("<no-annotation-name>")
                            calleeReference = buildSimpleNamedReference {
                                source = (entry.typeReference?.typeElement as? KtUserType)?.referenceExpression?.toFirSourceElement()
                                this.name = name
                            }
                            entry.extractArgumentsTo(this)
                            typeArguments.appendTypeArguments(entry.typeArguments)
                            containingDeclarationSymbol = context.containerSymbol
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
                        is KtLambdaArgument -> argumentExpression.apply {
                            // TODO(KT-66553) remove and set in builder
                            @OptIn(RawFirApi::class)
                            (this as? FirAnonymousFunctionExpression)?.replaceIsTrailingLambda(true)
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
            type: FirTypeRef,
            fieldOrd: Int,
        ): FirField {
            val delegateSource = entry.toFirSourceElement(KtFakeSourceElementKind.ClassDelegationField)

            return buildField {
                source = delegateSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Synthetic.DelegateField
                name = NameUtils.delegateFieldName(fieldOrd)
                symbol = FirFieldSymbol(CallableId(this@PsiRawFirBuilder.context.currentClassId, name))
                returnTypeRef = type
                withContainerSymbol(symbol) {
                    initializer = buildOrLazyExpression(delegateSource) {
                        { entry.delegateExpression }
                            .toFirExpression("Should have delegate", sourceWhenInvalidExpression = entry)
                    }
                }

                isVar = false
                status = FirDeclarationStatusImpl(Visibilities.Private, Modality.FINAL)
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

                        val delegateField = buildFieldForSupertypeDelegate(
                            superTypeListEntry, type, delegateFieldsMap.size
                        )
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
                        coneType = ConeClassLikeTypeImpl(
                            implicitEnumType.coneType.lookupTag,
                            delegatedSelfTypeRef?.coneType?.let { arrayOf(it) } ?: emptyArray(),
                            isMarkedNullable = false,
                        )
                        source = container.source?.fakeElement(KtFakeSourceElementKind.EnumSuperTypeRef)
                    }
                    container.superTypeRefs += delegatedSuperTypeRef!!
                }
                this is KtClass && classKind == ClassKind.ANNOTATION_CLASS -> {
                    container.superTypeRefs += implicitAnnotationType
                    delegatedSuperTypeRef = implicitAnyType
                }
            }

            val constructedClassId = this@PsiRawFirBuilder.context.currentClassId
            val isKotlinAny = constructedClassId == StandardClassIds.Any
            val defaultDelegatedSuperTypeRef =
                when {
                    classKind == ClassKind.ENUM_ENTRY && this is KtClass -> delegatedEnumSuperTypeRef ?: implicitAnyType
                    container.superTypeRefs.isEmpty() && !isKotlinAny -> implicitAnyType
                    else -> FirImplicitTypeRefImplWithoutSource
                }

            if (container.superTypeRefs.isEmpty() && !isKotlinAny) {
                val classIsKotlinNothing = constructedClassId == StandardClassIds.Nothing
                // kotlin.Nothing doesn't have `Any` supertype, but does have delegating constructor call to Any
                if (!classIsKotlinNothing) {
                    container.superTypeRefs += implicitAnyType
                }
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
                    isImplicitlyActual = isImplicitlyActual(container.status, classKind),
                    isKotlinAny = isKotlinAny,
                )
                container.declarations += firPrimaryConstructor
            }

            delegateFieldsMap.values.mapTo(container.declarations) { it.fir }
            return delegatedSuperTypeRef!! to delegateFieldsMap.takeIf { it.isNotEmpty() }
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

                val generateDelegatedSuperCall = shouldGenerateDelegatedSuperCall(
                    isAnySuperCall = isKotlinAny,
                    isExpectClass = containingClassIsExpectClass,
                    isEnumEntry = owner is KtEnumEntry,
                    hasExplicitDelegatedCalls = allSuperTypeCallEntries.isNotEmpty()
                )

                val firDelegatedCall = runIf(generateDelegatedSuperCall) {
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

                val explicitVisibility = this?.constructorExplicitVisibility
                val status = FirDeclarationStatusImpl(explicitVisibility ?: constructorDefaultVisibility(owner), Modality.FINAL).apply {
                    isExpect = this@toFirConstructor?.hasExpectModifier() == true || this@PsiRawFirBuilder.context.containerIsExpect
                    isActual = this@toFirConstructor?.hasActualModifier() == true || isImplicitlyActual

                    // a warning about inner script class is reported on the class itself
                    isInner = owner.parent.parent !is KtScript && owner.hasInnerModifier()
                    isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                    isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                }

                val builder = when {
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
                    this.contextParameters.addContextParameters(owner.contextReceiverLists, constructorSymbol)
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
            if (hasInnerModifier()) dispatchReceiverForInnerClassConstructor() else null

        override fun visitKtFile(file: KtFile, data: FirElement?): FirElement {
            context.packageFqName = when (mode) {
                BodyBuildingMode.NORMAL -> file.properPackageFqName
                BodyBuildingMode.LAZY_BODIES -> file.stub?.getPackageFqName() ?: file.properPackageFqName
            }
            return buildFile {
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

                file.fileAnnotationList?.let {
                    withContainerSymbol(symbol) {
                        for (annotationEntry in it.annotationEntries) {
                            annotations += annotationEntry.convert<FirAnnotation>()
                        }
                    }
                }

                for (importDirective in file.importDirectives) {
                    imports += buildImport {
                        source = importDirective.toFirSourceElement()
                        importedFqName = importDirective.importedFqName
                        isAllUnder = importDirective.isAllUnder
                        aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                        aliasSource = importDirective.alias?.toFirSourceElement()
                    }
                }

                if (file is KtCodeFragment) {
                    declarations += convertCodeFragment(file, this)
                } else {
                    for (declaration in file.declarations) {
                        declarations += when (declaration) {
                            is KtScript -> convertScriptOrSnippets(declaration, this@buildFile)
                            is KtDestructuringDeclaration -> {
                                val initializer = declaration.toInitializerExpression()
                                buildErrorNonLocalDestructuringDeclaration(declaration.toFirSourceElement(), initializer)
                            }
                            else -> declaration.convert()
                        }
                    }

                    for (danglingModifierList in file.danglingModifierLists) {
                        declarations += buildErrorNonLocalDeclarationForDanglingModifierList(danglingModifierList)
                    }
                }
            }
        }

        private fun convertScriptOrSnippets(declaration: KtScript, fileBuilder: FirFileBuilder): FirDeclaration {
            val file = declaration.parent as? KtFile
            requireWithAttachment(
                file?.declarations?.size == 1,
                message = { "Expect the script to be the only declaration in the file ${file?.name}" },
            ) {
                withEntry("fileName", fileBuilder.name)
            }

            val scriptSource = declaration.toFirSourceElement()

            val repSnippetConfigurator =
                baseSession.extensionService.replSnippetConfigurators.filter {
                    it.isReplSnippetsSource(fileBuilder.sourceFile, scriptSource)
                }.let {
                    requireWithAttachment(
                        it.size <= 1,
                        message = { "More than one REPL snippet configurator is found for the file" },
                    ) {
                        withEntry("fileName", fileBuilder.name)
                        withEntry("configurators", it.joinToString { "${it::class.java.name}" })
                    }
                    it.firstOrNull()
                }

            return if (repSnippetConfigurator != null) {
                convertReplSnippet(declaration, scriptSource, fileBuilder.name) {
                    with (repSnippetConfigurator) {
                        configureContainingFile(fileBuilder)
                        configure(fileBuilder.sourceFile, context)
                    }
                }
            } else {
                val scriptConfigurator =
                    baseSession.extensionService.scriptConfigurators.firstOrNull { it.accepts(fileBuilder.sourceFile, scriptSource) }

                convertScript(declaration, scriptSource, fileBuilder.name) {
                    if (scriptConfigurator != null) {
                        with(scriptConfigurator) {
                            configureContainingFile(fileBuilder)
                            configure(fileBuilder.sourceFile, context)
                        }
                    }
                }
            }
        }

        private val KtFile.properPackageFqName: FqName
            get() = packageDirective?.let(::parsePackageName) ?: FqName.ROOT

        private fun parsePackageName(node: KtPackageDirective): FqName {
            var packageName: FqName = FqName.ROOT
            val parts = node.getPackageNames()

            for (part in parts) {
                packageName = packageName.child(Name.identifier(part.getReferencedName()))
            }

            return packageName
        }

        protected fun configureScriptDestructuringDeclarationEntry(declaration: FirVariable, container: FirVariable) {
            (declaration as FirProperty).destructuringDeclarationContainerVariable = container.symbol
        }

        protected fun buildScriptDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration): FirVariable {
            val initializer = destructuringDeclaration.initializer
            val firInitializer = buildOrLazyExpression(initializer?.toFirSourceElement()) {
                initializer.toFirExpression("Initializer required for destructuring declaration", sourceWhenInvalidExpression = destructuringDeclaration)
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
            scriptSource: KtPsiSourceElement,
            fileName: String,
            setup: FirScriptBuilder.() -> Unit,
        ): FirScript {
            val scriptName = firScriptName(fileName)
            val scriptSymbol = FirScriptSymbol(context.packageFqName.child(scriptName))


            return buildScript {
                source = scriptSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                name = scriptName
                symbol = scriptSymbol

                val scriptDeclarationsIter = script.declarations.listIterator()
                withContainerScriptSymbol(symbol) {
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
                                    // the last anonymous initializer could be converted to a property and its symbol will be dropped
                                    // therefore we should not rely on it as a containing declaration symbol, and use the parent one instead
                                    isLocal = isLast,
                                )

                                declarations.add(initializer)
                            }
                            is KtDestructuringDeclaration -> {
                                val destructuringContainerVar = buildScriptDestructuringDeclaration(declaration)
                                declarations.add(destructuringContainerVar)

                                addDestructuringVariables(
                                    declarations,
                                    this@Visitor,
                                    moduleData,
                                    declaration,
                                    destructuringContainerVar,
                                    tmpVariable = false,
                                    forceLocal = false,
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
                }
            }
        }

        private fun convertReplSnippet(
            script: KtScript,
            scriptSource: KtPsiSourceElement,
            fileName: String,
            setup: FirReplSnippetBuilder.() -> Unit = {},
        ): FirReplSnippet {
            val snippetName = Name.special("<$fileName>")
            val snippetSymbol = FirReplSnippetSymbol(snippetName)

            return withContainerReplSymbol(snippetSymbol) {
                buildReplSnippet {
                    source = scriptSource
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    name = snippetName
                    symbol = snippetSymbol

                    body = buildOrLazyBlock {
                        // see KT-75301 for discussion about `isLocal` here
                        withContainerSymbol(snippetSymbol, isLocal = true) {
                            buildBlock {
                                withForcedLocalContext {
                                    script.declarations.forEach { declaration ->
                                        when (declaration) {
                                            is KtScriptInitializer -> {
                                                val initializer = buildAnonymousInitializer(
                                                    initializer = declaration,
                                                    containingDeclarationSymbol = snippetSymbol,
                                                    allowLazyBody = true,
                                                    isLocal = true,
                                                )

                                                statements.addAll(initializer.body!!.statements)
                                            }
                                            is KtDestructuringDeclaration -> {
                                                val destructuringContainerVar = buildScriptDestructuringDeclaration(declaration)
                                                statements.add(destructuringContainerVar)

                                                addDestructuringVariables(
                                                    statements,
                                                    this@Visitor,
                                                    baseModuleData,
                                                    declaration,
                                                    destructuringContainerVar,
                                                    tmpVariable = false,
                                                    forceLocal = false,
                                                ) {
                                                    configureScriptDestructuringDeclarationEntry(it, destructuringContainerVar)
                                                }
                                            }
                                            is KtProperty -> {
                                                val firProperty = convertProperty(declaration, null, forceLocal = true)
                                                firProperty.accept(snippetDeclarationVisitor)
                                                statements.add(firProperty)
                                            }
                                            else -> {
                                                val firStatement = declaration.toFirStatement()
                                                if (firStatement is FirDeclaration) {
                                                    firStatement.accept(snippetDeclarationVisitor)
                                                    statements.add(firStatement)
                                                } else {
                                                    error("unexpected declaration type in script")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // TODO: proper lazy support - see the script
                    resultTypeRef =
                        if (body.statements.lastOrNull() is FirDeclaration) implicitUnitType else FirImplicitTypeRefImplWithoutSource
                    setup()
                }
            }
        }

        private fun convertCodeFragment(
            file: KtCodeFragment,
            // We ask to pass the fileBuilder explicitly despite it's not in use: FirFile should be always a parent
            @Suppress("unused") fileBuilder: FirFileBuilder
        ): FirCodeFragment = buildCodeFragment {
            source = file.toFirSourceElement()
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            symbol = FirCodeFragmentSymbol()
            withContainerSymbol(symbol) {
                block = buildOrLazyBlock {
                    withForcedLocalContext {
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
                val functionSymbol = FirAnonymousFunctionSymbol()

                statements += buildAnonymousFunctionExpression {
                    anonymousFunction = buildAnonymousFunction {
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        source = file.toFirSourceElement(KtFakeSourceElementKind.CodeFragment)
                        symbol = functionSymbol

                        hasExplicitParameterList = true
                        isLambda = false

                        // Place the type reference in the parameter position
                        // so it doesn't require special handling in the function body
                        valueParameters += buildValueParameter {
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            source = file.toFirSourceElement(KtFakeSourceElementKind.CodeFragment)
                            name = StandardNames.DEFAULT_VALUE_PARAMETER

                            symbol = FirValueParameterSymbol(name)
                            containingDeclarationSymbol = functionSymbol

                            returnTypeRef = file.getContentElement().toFirOrErrorType()
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }

                        returnTypeRef = implicitUnitType

                        body = buildBlock()
                    }
                }
            }
        }

        override fun visitScript(script: KtScript, data: FirElement?): FirElement {
            val ktFile = script.containingKtFile
            val fileName = ktFile.name
            val sourceFile = KtPsiSourceFile((data as? FirScript)?.psi?.containingFile as? KtFile ?: ktFile)
            val scriptSource = script.toFirSourceElement()
            val scriptConfigurator =
                baseSession.extensionService.scriptConfigurators.firstOrNull { it.accepts(sourceFile, scriptSource) }
            return convertScript(script, scriptSource, fileName) {
                scriptConfigurator?.run {
                    // TODO: looks like we may loose the implicit imports here, find out whether and how the file could be configured too (KT-73847)
//                    configureContainingFile(fileBuilder)
                    configure(sourceFile, context)
                }
            }
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
                                        coneType =
                                            ConeClassLikeTypeImpl(
                                                this@buildAnonymousObject.symbol.toLookupTag(),
                                                emptyArray(),
                                                isMarkedNullable = false
                                            )
                                        source = toFirSourceElement(KtFakeSourceElementKind.ClassSelfTypeRef)
                                    }
                                    registerSelfType(delegatedEntrySelfType)

                                    superTypeRefs += delegatedEnumSelfTypeRef
                                    val superTypeCallEntry = superTypeListEntries.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                                    val correctedEnumSelfTypeRef = buildResolvedTypeRef {
                                        source = superTypeCallEntry?.calleeExpression?.typeReference?.toFirSourceElement()
                                            ?: delegatedEntrySelfType.source
                                        coneType = delegatedEnumSelfTypeRef.coneType
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

                                        for (danglingModifier in ktEnumEntry.body?.danglingModifierLists.orEmpty()) {
                                            declarations += buildErrorNonLocalDeclarationForDanglingModifierList(danglingModifier).apply {
                                                containingClassAttr = currentDispatchReceiverType()?.lookupTag
                                            }
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

        private fun MutableList<FirValueParameter>.addContextParameters(
            contextLists: List<KtContextReceiverList>,
            containingDeclarationSymbol: FirBasedSymbol<*>,
        ) {
            for (contextList in contextLists) {
                contextList.contextParameters().mapTo(this) { contextParameterElement ->
                    contextParameterElement.toFirValueParameter(
                        defaultTypeRef = null,
                        containingDeclarationSymbol = containingDeclarationSymbol,
                        valueParameterDeclaration = ValueParameterDeclaration.CONTEXT_PARAMETER,
                    )
                }

                contextList.contextReceivers().mapTo(this) { contextReceiverElement ->
                    buildValueParameter {
                        this.source = contextReceiverElement.toFirSourceElement()
                        this.moduleData = baseModuleData
                        this.origin = FirDeclarationOrigin.Source

                        val customLabelName = contextReceiverElement.labelNameAsName()
                        val labelNameFromTypeRef = contextReceiverElement.typeReference()?.nameForReceiverLabel()?.let(Name::identifier)

                        // We're abusing the value parameter name for the label/type name of legacy context receivers.
                        // Luckily, legacy context receivers are getting removed soon.
                        this.name = customLabelName ?: labelNameFromTypeRef ?: SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                        this.symbol = FirValueParameterSymbol(name)
                        withContainerSymbol(this.symbol) {
                            this.returnTypeRef = contextReceiverElement.typeReference().toFirOrErrorType()
                        }
                        this.containingDeclarationSymbol = containingDeclarationSymbol
                        this.valueParameterKind = FirValueParameterKind.LegacyContextReceiver
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
                        isInner = classOrObject.hasInnerModifier() && classOrObject.parent.parent !is KtScript
                        isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
                        isData = classOrObject.hasModifier(DATA_KEYWORD)
                        isInline = classOrObject.hasModifier(INLINE_KEYWORD)
                        isValue = classOrObject.hasModifier(VALUE_KEYWORD)
                        isFun = classOrObject.hasModifier(FUN_KEYWORD)
                        isExternal = classOrObject.hasModifier(EXTERNAL_KEYWORD)
                    }
                    val firTypeParameters = classOrObject.convertTypeParameters(classSymbol)

                    withCapturedTypeParameters(
                        // Transferring phantom type parameters to objects is cursed as they are
                        // accessible by qualifier `MyObject`, which is an expression and must have
                        // some single type.
                        // Letting their types contain no type arguments while the class itself
                        // expects some sounds fragile.
                        status = status.isInner || isLocal && !classKind.isObject,
                        declarationSource = sourceElement,
                        currentFirTypeParameters = firTypeParameters,
                    ) {
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
                            typeParameters += firTypeParameters

                            classOrObject.extractAnnotationsTo(this)
                            context.appendOuterTypeParameters(ignoreLastLevel = true, typeParameters)

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
                                    buildErrorNonLocalDeclarationForDanglingModifierList(danglingModifier).apply {
                                        containingClassAttr = currentDispatchReceiverType()?.lookupTag
                                    }
                                )
                            }

                            if (classOrObject.hasModifier(DATA_KEYWORD) && firPrimaryConstructor != null) {
                                val zippedParameters =
                                    classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() } zip declarations.filterIsInstance<FirProperty>()
                                DataClassMembersGenerator(
                                    classOrObject.primaryConstructor ?: classOrObject,
                                    this,
                                    firPrimaryConstructor,
                                    zippedParameters,
                                    context.packageFqName,
                                    context.className,
                                    addValueParameterAnnotations = {
                                        withContainerSymbol(symbol) {
                                            addAnnotationsFrom(
                                                it as KtParameter,
                                                isFromPrimaryConstructor = true,
                                            )
                                        }
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

                            contextParameters.addContextParameters(classOrObject.contextReceiverLists, classSymbol)
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
                context.containingScriptSymbol?.let { script ->
                    it.containingScriptSymbolAttr = script
                }
                context.containingReplSymbol?.let { repl ->
                    it.containingReplSymbolAttr = repl
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
                            declarations += buildErrorNonLocalDeclarationForDanglingModifierList(danglingModifier).apply {
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
                    val isInner = typeAlias.hasInnerModifier()
                    withContainerSymbol(symbol) {
                        source = typeAlias.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        scopeProvider = this@PsiRawFirBuilder.baseScopeProvider
                        name = typeAlias.nameAsSafeName
                        val isLocal = context.inLocalContext
                        status = FirDeclarationStatusImpl(
                            if (isLocal) Visibilities.Local else typeAlias.getVisibility(publicByDefault = true),
                            Modality.FINAL
                        ).apply {
                            isExpect = typeAliasIsExpect
                            isActual = typeAlias.hasActualModifier()
                            this.isInner = isInner
                        }
                        expandedTypeRef = typeAlias.getTypeReference().toFirOrErrorType()
                        typeAlias.extractAnnotationsTo(this)
                        typeParameters += typeAlias.convertTypeParameters(symbol)

                        if (isInner || isLocal) {
                            context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
                        }
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

                val receiverTypeCalculator: (() -> FirTypeRef)? = function.receiverTypeReference?.let {
                    { it.toFirType() }
                }

                val labelName: String?

                val functionBuilder = if (isAnonymousFunction) {
                    FirAnonymousFunctionBuilder().apply {
                        staticReceiverParameter = function.staticReceiverType?.toUserTypeRef()
                        receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, baseModuleData, functionSymbol) }
                        symbol = functionSymbol as FirAnonymousFunctionSymbol
                        isLambda = false
                        hasExplicitParameterList = true
                        label = context.getLastLabel(function)
                        labelName = label?.name ?: context.calleeNamesForLambda.lastOrNull()?.identifier

                        val isExpect = function.hasExpectModifier() || context.containerIsExpect
                        val isActual = function.hasActualModifier()
                        val isOverride = function.hasModifier(OVERRIDE_KEYWORD)
                        val isOperator = function.hasModifier(OPERATOR_KEYWORD)
                        val isInfix = function.hasModifier(INFIX_KEYWORD)
                        val isInline = function.hasModifier(INLINE_KEYWORD)
                        val isTailRec = function.hasModifier(TAILREC_KEYWORD)
                        val isExternal = function.hasModifier(EXTERNAL_KEYWORD)
                        val isSuspend = function.hasModifier(SUSPEND_KEYWORD)
                        val isStatic = function.hasModifier(STATIC_KEYWORD)

                        if (isExpect || isActual || isOverride || isOperator || isInfix || isInline || isTailRec || isExternal || isSuspend) {
                            status = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.copy(
                                isExpect = isExpect,
                                isActual = isActual,
                                isOverride = isOverride,
                                isOperator = isOperator,
                                isInfix = isInfix,
                                isInline = isInline,
                                isTailRec = isTailRec,
                                isExternal = isExternal,
                                isSuspend = isSuspend,
                                isStatic = isStatic
                            )
                        }
                    }
                } else {
                    FirSimpleFunctionBuilder().apply {
                        staticReceiverParameter = function.staticReceiverType?.toUserTypeRef()
                        receiverParameter = receiverTypeCalculator?.let { createReceiverParameter(it, baseModuleData, functionSymbol) }
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
                            isStatic = function.hasModifier(STATIC_KEYWORD)
                        }
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

                    function.extractTypeParametersTo(this, functionSymbol)
                    contextParameters.addContextParameters(function.contextReceiverLists, functionSymbol)
                    for (valueParameter in function.valueParameters) {
                        valueParameters += valueParameter.toFirValueParameter(
                            null,
                            functionSymbol,
                            if (isAnonymousFunction) ValueParameterDeclaration.LAMBDA else ValueParameterDeclaration.FUNCTION,
                        )
                    }

                    withCapturedTypeParameters(true, functionSource, typeParameters) {
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
                    function.fillDanglingConstraintsTo(it)
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
                symbol = FirAnonymousFunctionSymbol()
                receiverParameter = literalSource.asReceiverParameter(moduleData, symbol)
                isLambda = true
                hasExplicitParameterList = expression.functionLiteral.arrow != null

                val destructuringVariables = mutableListOf<FirStatement>()
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val name = SpecialNames.DESTRUCT
                        val multiParameter = buildValueParameter {
                            source = valueParameter.toFirSourceElement()
                            containingDeclarationSymbol = this@buildAnonymousFunction.symbol
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = valueParameter.typeReference.toFirOrImplicitType()
                            this.name = name
                            symbol = FirValueParameterSymbol(name)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                        addDestructuringVariables(
                            destructuringVariables,
                            this@Visitor,
                            baseModuleData,
                            multiDeclaration,
                            multiParameter,
                            tmpVariable = false,
                            forceLocal = true,
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
                        val errorExpression = buildErrorExpression(literalSource, ConeSyntaxDiagnostic("Lambda has no body"))
                        FirSingleExpressionBlock(errorExpression.toReturn())
                    } else {
                        val kind = runIf(destructuringVariables.isNotEmpty()) {
                            KtFakeSourceElementKind.LambdaDestructuringBlock
                        }
                        val bodyBlock = configureBlockWithoutBuilding(ktBody, kind).apply {
                            if (statements.isEmpty()) {
                                statements.add(
                                    buildReturnExpression {
                                        source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody)
                                        this.target = target
                                        result = buildUnitExpression {
                                            source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitUnit.ForEmptyLambda)
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
                    val explicitVisibility = constructorExplicitVisibility
                    status = FirDeclarationStatusImpl(explicitVisibility ?: constructorDefaultVisibility(owner), Modality.FINAL).apply {
                        isExpect = hasExpectModifier() || this@PsiRawFirBuilder.context.containerIsExpect
                        isActual = hasActualModifier()
                        isInner = owner.hasInnerModifier()
                        isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                        isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                    }
                    dispatchReceiverType = owner.obtainDispatchReceiverForConstructor()
                    contextParameters.addContextParameters(owner.contextReceiverLists, symbol)
                    contextParameters.addContextParameters(this@toFirConstructor.modifierList?.contextReceiverLists.orEmpty(), symbol)
                    if (!owner.hasModifier(EXTERNAL_KEYWORD) && !status.isExpect || isExplicitDelegationCall()) {
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

        protected fun KtDeclarationWithInitializer.toInitializerExpression(): FirExpression? =
            runIf(hasInitializer()) {
                this@PsiRawFirBuilder.context.calleeNamesForLambda += null

                val expression = buildOrLazyExpression(null) {
                    withForcedLocalContext {
                        initializer.toFirExpression("Should have initializer", sourceWhenInvalidExpression = this)
                    }
                }

                this@PsiRawFirBuilder.context.calleeNamesForLambda.removeLast()
                expression
            }

        private fun <T> KtProperty.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            context: Context<T>,
            forceLocal: Boolean = false,
        ): FirProperty {
            val isInsideScript = context.containingScriptSymbol != null && context.className == FqName.ROOT
            val propertyName = when {
                (isLocal || isInsideScript) && nameIdentifier?.text == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                else -> nameAsSafeName
            }
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

                    receiverParameter = receiverTypeReference?.let {
                        createReceiverParameter({ it.toFirType() }, moduleData, propertySymbol)
                    }

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
                                this@toFirProperty.delegate?.expression.toFirExpression(
                                    "Incorrect delegate expression",
                                    sourceWhenInvalidExpression = this@toFirProperty
                                )

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
                                explicitDeclarationSource = propertySource,
                            )
                        }
                    } else {
                        isLocal = forceLocal
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
                                isStatic = hasModifier(STATIC_KEYWORD)
                            }

                            if (hasDelegate()) {
                                val fakeDelegateSource = this@toFirProperty.toFirSourceElement(KtFakeSourceElementKind.WrappedDelegate)
                                fun extractDelegateExpression(): FirExpression = buildOrLazyExpression(fakeDelegateSource) {
                                    this@toFirProperty.delegate?.expression.toFirExpression(
                                        "Should have delegate",
                                        sourceWhenInvalidExpression = this@toFirProperty
                                    )
                                }

                                val delegateBuilder = FirWrappedDelegateExpressionBuilder().apply {
                                    val delegateExpression = extractDelegateExpression()
                                    source = buildOrLazy(
                                        build = {
                                            val psiPropertyDelegate = this@toFirProperty.delegate
                                            (psiPropertyDelegate?.expression ?: psiPropertyDelegate)?.toFirSourceElement(
                                                KtFakeSourceElementKind.WrappedDelegate
                                            )
                                        },
                                        lazy = { fakeDelegateSource },
                                    )

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
                                    bindFunction = ::bindFunctionTarget,
                                    explicitDeclarationSource = propertySource,
                                )
                            }
                        }
                    }
                    annotations += when {
                        isLocal -> propertyAnnotations
                        else -> propertyAnnotations.filterStandalonePropertyRelevantAnnotations(isVar)
                    }

                    contextParameters.addContextParameters(this@toFirProperty.contextReceiverLists, propertySymbol)
                }.also {
                    if (!isLocal) {
                        fillDanglingConstraintsTo(it)
                    }
                }
            }
        }

        /**
         * Builds [FirAnonymousInitializer] from [KtAnonymousInitializer]
         *
         * @param initializer Source [KtAnonymousInitializer]
         * @param containingDeclarationSymbol containing declaration symbol, if any
         * @param allowLazyBody if `true`, [FirLazyBlock] is used in the IDE mode
         * @param isLocal if `true`, the initializer is not used as a containing declaration for the contents of the initializer
         */
        protected fun buildAnonymousInitializer(
            initializer: KtAnonymousInitializer,
            containingDeclarationSymbol: FirBasedSymbol<*>,
            allowLazyBody: Boolean = true,
            isLocal: Boolean = false,
        ): FirAnonymousInitializer = buildAnonymousInitializer {
            withContainerSymbol(symbol, isLocal) {
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

                        val contextReceiverList = unwrappedElement.contextReceiverList
                        contextReceiverList?.contextReceivers()?.mapNotNullTo(contextParameterTypeRefs) {
                            it.typeReference()?.toFirType()
                        }
                        contextReceiverList?.contextParameters()?.mapNotNullTo(contextParameterTypeRefs) {
                            it.typeReference?.toFirType()
                        }
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

        private fun KtUserType.toUserTypeRef(): FirUserTypeRef {
            val source = toFirSourceElement()
            val referenceExpression = referenceExpression!!
            return convertKtTypeElement(source, false, this, referenceExpression).build()
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

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: FirElement?): FirElement = withForcedLocalContext {
            val annotationUseSiteTarget = annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()

            if (annotationUseSiteTarget == ALL && annotationEntry.parent is KtAnnotation) {
                buildErrorAnnotationCall {
                    // Intentionally forbidden @all:[A1 A2] case
                    source = annotationEntry.toFirSourceElement()
                    useSiteTarget = annotationUseSiteTarget
                    annotationTypeRef = annotationEntry.typeReference.toFirOrErrorType()
                    annotationEntry.extractArgumentsTo(this)
                    val name = (annotationTypeRef as? FirUserTypeRef)?.qualifier?.last()?.name ?: Name.special("<no-annotation-name>")
                    calleeReference = buildSimpleNamedReference {
                        source = (annotationEntry.typeReference?.typeElement as? KtUserType)?.referenceExpression?.toFirSourceElement()
                        this.name = name
                    }
                    typeArguments.appendTypeArguments(annotationEntry.typeArguments)
                    containingDeclarationSymbol = context.containerSymbol
                    diagnostic = ConeSimpleDiagnostic(
                        "Multiple annotation syntax with @all use-site target is forbidden",
                        DiagnosticKind.MultipleAnnotationWithAllTarget
                    )
                }
            } else {
                buildAnnotationCall {
                    source = annotationEntry.toFirSourceElement()
                    useSiteTarget = annotationUseSiteTarget
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

            return generateAccessExpression(
                qualifiedSource,
                expression.toFirSourceElement(),
                expression.getReferencedNameAsName(),
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
                convertTemplateEntry = { errorReason ->
                    (this as KtStringTemplateEntryWithExpression).expressions.map { it.toFirExpression(errorReason) }
                },
                prefix = { expression.interpolationPrefix?.text ?: "" },
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

                val ktCondition = expression.condition
                branches += buildRegularWhenBranch {
                    source = ktCondition?.toFirSourceElement(KtFakeSourceElementKind.WhenCondition)
                    condition = ktCondition.toFirExpression("If statement should have condition", sourceWhenInvalidExpression = expression)
                    result = expression.then.toFirBlock()
                }

                if (expression.`else` != null) {
                    branches += buildRegularWhenBranch {
                        source = expression.elseKeyword?.toKtPsiSourceElement()
                        condition = buildElseIfTrueCondition()
                        result = expression.`else`.toFirBlock()
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
            var subjectVariable = when (ktSubjectExpression) {
                is KtVariableDeclaration -> {
                    val name = when {
                        ktSubjectExpression.nameIdentifier?.text == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                        else -> ktSubjectExpression.nameAsSafeName
                    }
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
                        receiverParameter = ktSubjectExpression.receiverTypeReference?.let {
                            createReceiverParameter({ it.toFirType() }, moduleData, symbol)
                        }
                        ktSubjectExpression.extractAnnotationsTo(this)
                    }
                }
                else -> null
            }
            val hasSubject = subjectExpression != null

            if (hasSubject && subjectVariable == null) {
                val name = SpecialNames.WHEN_SUBJECT
                subjectVariable = buildProperty {
                    source = subjectExpression.source?.fakeElement(KtFakeSourceElementKind.WhenGeneratedSubject)
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Synthetic.ImplicitWhenSubject
                    returnTypeRef = FirImplicitTypeRefImplWithoutSource
                    this.name = name
                    initializer = subjectExpression
                    delegate = null
                    isVar = false
                    symbol = FirPropertySymbol(name)
                    isLocal = true
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                }
            }

            @OptIn(FirContractViolation::class)
            val ref = FirExpressionRef<FirWhenExpression>()
            var shouldBind = hasSubject
            return buildWhenExpression {
                source = expression.toFirSourceElement()
                this.subjectVariable = subjectVariable
                usedAsExpression = expression.usedAsExpression

                for (entry in expression.entries) {
                    val entrySource = entry.toFirSourceElement()
                    val entryGuard = entry.guard?.let { it.getExpression().toFirExpression("No expression in guard", sourceWhenInvalidExpression = it) }
                    val branchBody = entry.expression.toFirBlock()
                    branches += if (entry.elseKeyword == null) {
                        if (hasSubject) {
                            buildWhenBranch(hasGuard = entryGuard != null) {
                                source = entrySource
                                condition = entry.conditions.toFirWhenCondition(
                                    subjectVariable,
                                    { errorReason, fallbackSource ->
                                        toFirExpression(errorReason, sourceWhenInvalidExpression = fallbackSource)
                                    },
                                    { toFirOrErrorType() },
                                ).guardedBy(entryGuard)
                                result = branchBody
                            }
                        } else {
                            val ktCondition = entry.conditions.first()
                            buildWhenBranch(hasGuard = entryGuard != null) {
                                source = entrySource
                                condition =
                                    if (entry.conditions.size == 1 && ktCondition is KtWhenConditionWithExpression) {
                                        (ktCondition.expression ?: ktCondition).toFirExpression("No expression in condition with expression")
                                    } else {
                                        buildBalancedOrExpressionTree(entry.conditions.map { condition ->
                                            if (condition is KtWhenConditionWithExpression) {
                                                condition.expression.toFirExpression(
                                                    "No expression in condition with expression",
                                                    sourceWhenInvalidExpression = condition
                                                )
                                            } else {
                                                shouldBind = true
                                                val convertedCondition = condition.toFirWhenCondition(
                                                    subjectVariable,
                                                    { errorReason, fallbackSource ->
                                                            toFirExpression(errorReason, sourceWhenInvalidExpression = fallbackSource)
                                                        },
                                                    { toFirOrErrorType() },
                                                )
                                                convertedCondition.takeIf { subjectVariable != null }
                                                    ?: buildErrorExpression {
                                                        source = condition.toFirSourceElement()
                                                        this.nonExpressionElement = convertedCondition
                                                        diagnostic = ConeSimpleDiagnostic(
                                                            "No expression in condition with expression",
                                                            DiagnosticKind.ExpressionExpected,
                                                        )
                                                    }
                                            }
                                        })
                                    }.guardedBy(entryGuard)
                                result = branchBody
                            }
                        }
                    } else {
                        buildWhenBranch(hasGuard = entryGuard != null) {
                            source = entrySource
                            condition = entryGuard ?: buildElseIfTrueCondition()
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

                when (parent.elementType) {
                    KtNodeTypes.THEN, KtNodeTypes.ELSE, KtNodeTypes.WHEN_ENTRY -> {
                        return (parent.parent as? KtExpression)?.usedAsExpression != false
                    }
                }

                fun PsiElement.getLastChildExpression() = children.asList().asReversed().firstIsInstanceOrNull<KtExpression>()

                return when (parent) {
                    is KtBlockExpression, is KtTryExpression -> parent.getLastChildExpression() == this && parent.usedAsExpression
                    is KtCatchClause -> (parent.parent as? KtTryExpression)?.usedAsExpression == true
                    is KtClassInitializer, is KtScriptInitializer, is KtSecondaryConstructor, is KtFunctionLiteral, is KtFinallySection -> false
                    is KtDotQualifiedExpression -> parent.firstChild == this
                    is KtFunction, is KtPropertyAccessor -> parent.hasBody() && !parent.hasBlockBody()
                    is KtContainerNodeForControlStructureBody -> when (parent.parent.elementType) {
                        KtNodeTypes.FOR, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE -> false
                        else -> true
                    }
                    else -> true
                }
            }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: FirElement?): FirElement {
            val target: FirLoopTarget
            return FirDoWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
                target = prepareTarget(expression)
                condition = expression.condition.toFirExpression(
                    "No condition in do-while loop",
                    sourceWhenInvalidExpression = expression.getChildNodeByType(KtNodeTypes.CONDITION) as? KtElement ?: expression
                )
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: FirElement?): FirElement {
            val target: FirLoopTarget
            return FirWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                condition = expression.condition.toFirExpression(
                    "No condition in while loop",
                    sourceWhenInvalidExpression = expression.getChildNodeByType(KtNodeTypes.CONDITION) as? KtElement ?: expression
                )
                // break/continue in the while loop condition will refer to an outer loop if any.
                // So, prepare the loop target after building the condition.
                target = prepareTarget(expression)
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: FirElement?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression(
                "No range in for loop",
                sourceWhenInvalidExpression = expression.getChildNodeByType(KtNodeTypes.LOOP_RANGE) as? KtElement ?: expression
            )
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
                        source = expression.toFirSourceElement()
                    }
                    if (ktParameter != null) {
                        val multiDeclaration = ktParameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            moduleData = baseModuleData,
                            source = ktParameter.toFirSourceElement(),
                            name = when {
                                multiDeclaration != null -> SpecialNames.DESTRUCT
                                ktParameter.nameIdentifier?.asText == "_" -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                                else -> ktParameter.nameAsSafeName
                            },
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
                        ).apply {
                            isForLoopParameter = true
                        }
                        if (multiDeclaration != null) {
                            addDestructuringVariables(
                                blockBuilder.statements,
                                this@Visitor,
                                baseModuleData,
                                multiDeclaration = multiDeclaration,
                                container = firLoopParameter,
                                tmpVariable = true,
                                forceLocal = true,
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
            val foldingStringConcatenationStack = expression.tryVisitFoldingStringConcatenation()

            return if (foldingStringConcatenationStack != null) {
                buildStringConcatenationCall {
                    val stringConcatenationSource = expression.toFirSourceElement()
                    argumentList = buildArgumentList {
                        foldingStringConcatenationStack.mapTo(arguments) { it.convert() }
                        source = stringConcatenationSource
                    }
                    source = stringConcatenationSource
                    interpolationPrefix = ""
                    isFoldedStrings = true
                }
            } else {
                visitBinaryExpressionFallback(expression)
            }
        }

        private fun visitBinaryExpressionFallback(expression: KtBinaryExpression): FirElement {
            val operationToken = expression.operationToken

            if (operationToken == IDENTIFIER) {
                context.calleeNamesForLambda += expression.operationReference.getReferencedNameAsName()
            } else {
                context.calleeNamesForLambda += null
            }

            val leftArgument = expression.left.toFirExpression("No left operand", sourceWhenInvalidExpression = expression)
            val rightArgument = expression.right.toFirExpression("No right operand", sourceWhenInvalidExpression = expression)

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
                        expression.left?.elementType in UNWRAPPABLE_TOKEN_TYPES,
                    ) {
                        (this as KtExpression).toFirExpression(
                            sourceWhenInvalidExpression = expression,
                            isValidExpression = { !it.isStatementLikeExpression || it.isArraySet },
                        ) { missing ->
                            val message = "Incorrect expression in assignment"
                            if (missing) {
                                ConeSyntaxDiagnostic(message)
                            } else {
                                ConeSimpleDiagnostic(message, DiagnosticKind.ExpressionExpected)
                            }
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
                        argumentList = buildUnaryArgumentList(argument.toFirExpression("No operand", sourceWhenInvalidExpression = expression))
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

                    val receiver = argument.toFirExpression("No operand", sourceWhenInvalidExpression = expression)

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

                calleeExpression is KtSuperExpression || parenthesizedArgument is FirSuperReceiverExpression -> {
                    CalleeAndReceiver(
                        buildErrorNamedReferenceWithNoName(
                            source = (calleeExpression as? KtSuperExpression)?.toFirSourceElement()
                                ?: (parenthesizedArgument as? FirResolvable)?.calleeReference?.source,
                            diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed),
                        )
                    )
                }

                parenthesizedArgument != null -> {
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = defaultSource.fakeElement(KtFakeSourceElementKind.ImplicitInvokeCall)
                            name = OperatorNameConventions.INVOKE
                        },
                        receiverForInvoke = parenthesizedArgument,
                    )
                }

                calleeExpression == null -> {
                    CalleeAndReceiver(
                        buildErrorNamedReferenceWithNoName(
                            source = defaultSource,
                            diagnostic = ConeSyntaxDiagnostic("Call has no callee"),
                        )
                    )
                }

                else -> {
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = defaultSource.fakeElement(KtFakeSourceElementKind.ImplicitInvokeCall)
                            name = OperatorNameConventions.INVOKE
                        },
                        receiverForInvoke = calleeExpression.toFirExpression("Incorrect invoke receiver"),
                    )
                }
            }
        }

        // In non-erroneous code, it's either `f()` (without explicit receiver) or `(expr)()` which is transformed to `expr.invoke()`
        override fun visitCallExpression(expression: KtCallExpression, data: FirElement?): FirElement {
            val source = expression.toFirSourceElement()
            val (calleeReference, receiverForInvoke) = splitToCalleeAndReceiver(expression.calleeExpression, source)

            val result: FirQualifiedAccessExpressionBuilder =
                if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
                    FirPropertyAccessExpressionBuilder().apply {
                        this.source = source
                        this.calleeReference = calleeReference
                    }
                } else {
                    val builder = if (receiverForInvoke != null) FirImplicitInvokeCallBuilder() else FirFunctionCallBuilder()
                    builder.apply {
                        this.source = source
                        this.calleeReference = calleeReference
                        context.calleeNamesForLambda += calleeReference.name
                        expression.extractArgumentsTo(this)
                        context.calleeNamesForLambda.removeLast()
                    }
                }

            return result.apply {
                this.explicitReceiver = receiverForInvoke
                typeArguments.appendTypeArguments(expression.typeArguments)
            }.build().pullUpSafeCallIfNecessary()
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
                explicitReceiver = arrayExpression.toFirExpression("No array expression", sourceWhenInvalidExpression = expression)
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
            return when (firSelector) {
                is FirQualifiedAccessExpression -> {
                    if (expression is KtSafeQualifiedExpression) {
                        @OptIn(FirImplementationDetail::class)
                        firSelector.replaceSource(expression.toFirSourceElement(KtFakeSourceElementKind.DesugaredSafeCallExpression))
                        return firSelector.createSafeCall(
                            receiver,
                            expression.toFirSourceElement()
                        )
                    }

                    convertFirSelector(firSelector, expression.toFirSourceElement(), receiver)
                }
                is FirErrorExpression -> {
                    buildQualifiedErrorAccessExpression {
                        this.receiver = receiver
                        this.selector = firSelector
                        source = expression.toFirSourceElement()
                        diagnostic = ConeSyntaxDiagnostic("Qualified expression with unexpected selector")
                    }
                }
                else -> {
                    firSelector
                }
            }
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
            return buildSuperReceiverExpression {
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
            return expression.expression.toFirExpression("Empty parentheses", sourceWhenInvalidExpression = expression)
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

            return buildExpressionHandlingLabelErrors(result, expression.toFirSourceElement(), forbiddenLabelKind, labelSource)
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
                exception = expression.thrownExpression.toFirExpression("Nothing to throw", sourceWhenInvalidExpression = expression)
            }
        }

        override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: FirElement?): FirElement {
            val baseVariable = generateTemporaryVariable(
                baseModuleData,
                multiDeclaration.toFirSourceElement(),
                "destruct",
                multiDeclaration.initializer.toFirExpression("Initializer required for destructuring declaration", sourceWhenInvalidExpression = multiDeclaration),
                extractAnnotationsTo = { extractAnnotationsTo(it) }
            )
            return generateDestructuringBlock(
                this@Visitor,
                baseModuleData,
                multiDeclaration,
                baseVariable,
                tmpVariable = true,
            )
        }

        override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: FirElement?): FirElement {
            return buildGetClassCall {
                source = expression.toFirSourceElement()
                argumentList = buildUnaryArgumentList(
                    expression.receiverExpression.toFirExpression(sourceWhenInvalidExpression = expression) {
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

        private fun buildErrorNonLocalDeclarationForDanglingModifierList(modifierList: KtModifierList) = buildDanglingModifierList {
            this.source = modifierList.toFirSourceElement(KtFakeSourceElementKind.DanglingModifierList)
            moduleData = baseModuleData
            origin = FirDeclarationOrigin.Source
            diagnostic = ConeDanglingModifierOnTopLevel
            symbol = FirDanglingModifierSymbol()
            withContainerSymbol(symbol) {
                for (annotationEntry in modifierList.annotationEntries) {
                    annotations += annotationEntry.convert<FirAnnotation>()
                }

                contextParameters.addContextParameters(modifierList.contextReceiverLists, symbol)
            }
        }
    }

    companion object {
        fun firScriptName(fileName: String): Name = Name.special("<script-$fileName>")
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

private val snippetDeclarationVisitor: FirVisitorVoid = object : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {}

    override fun visitProperty(property: FirProperty) {
        property.isReplSnippetDeclaration = true
        property.getter?.accept(this)
        property.setter?.accept(this)
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        regularClass.isReplSnippetDeclaration = true
        regularClass.declarations.forEach {
            if (it is FirClass) it.accept(this)
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        simpleFunction.isReplSnippetDeclaration = true
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        propertyAccessor.isReplSnippetDeclaration = true
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        typeAlias.isReplSnippetDeclaration = true
    }
}
