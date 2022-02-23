/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildRawContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

open class RawFirBuilder(
    session: FirSession,
    val baseScopeProvider: FirScopeProvider,
    private val psiMode: PsiHandlingMode,
    bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL
) : BaseFirBuilder<PsiElement>(session) {

    @Deprecated("Please replace with primary constructor call")
    constructor(session: FirSession, baseScopeProvider: FirScopeProvider, mode: BodyBuildingMode = BodyBuildingMode.NORMAL) :
            this(session, baseScopeProvider, psiMode = PsiHandlingMode.IDE, bodyBuildingMode = mode)

    protected open fun bindFunctionTarget(target: FirFunctionTarget, function: FirFunction) = target.bind(function)

    var mode: BodyBuildingMode = bodyBuildingMode
        private set

    private inline fun <T> disabledLazyMode(body: () -> T): T {
        if (mode != BodyBuildingMode.LAZY_BODIES) return body()
        return try {
            mode = BodyBuildingMode.NORMAL
            body()
        } finally {
            mode = BodyBuildingMode.LAZY_BODIES
        }
    }

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

    fun buildTypeReference(reference: KtTypeReference): FirTypeRef {
        return reference.accept(Visitor(), Unit) as FirTypeRef
    }

    override fun PsiElement.toFirSourceElement(kind: KtFakeSourceElementKind?): KtPsiSourceElement {
        val actualKind = kind ?: this@RawFirBuilder.context.forcedElementSourceKind ?: KtRealSourceElementKind
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

    private val KtModifierListOwner.visibility: Visibility
        get() = with(modifierList) {
            when {
                this == null -> Visibilities.Unknown
                hasModifier(PRIVATE_KEYWORD) -> Visibilities.Private
                hasModifier(PUBLIC_KEYWORD) -> Visibilities.Public
                hasModifier(PROTECTED_KEYWORD) -> Visibilities.Protected
                else -> if (hasModifier(INTERNAL_KEYWORD)) Visibilities.Internal else Visibilities.Unknown
            }
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

    protected open inner class Visitor : KtVisitor<FirElement, Unit>() {
        private inline fun <reified R : FirElement> KtElement?.convertSafe(): R? =
            this?.let(::convertElement) as? R

        private inline fun <reified R : FirElement> KtElement.convert(): R =
            convertElement(this) as R

        open fun convertElement(element: KtElement): FirElement? =
            element.accept(this@Visitor, Unit)

        open fun convertProperty(
            property: KtProperty, ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            ownerRegularClassTypeParametersCount: Int?
        ): FirProperty = property.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol,
            ownerRegularClassTypeParametersCount,
            context
        )

        open fun convertValueParameter(
            valueParameter: KtParameter,
            defaultTypeRef: FirTypeRef? = null,
            valueParameterDeclaration: ValueParameterDeclaration = ValueParameterDeclaration.OTHER,
            additionalAnnotations: List<FirAnnotation> = emptyList()
        ): FirValueParameter = valueParameter.toFirValueParameter(defaultTypeRef, valueParameterDeclaration, additionalAnnotations)

        private fun KtTypeReference?.toFirOrImplicitType(): FirTypeRef =
            convertSafe() ?: buildImplicitTypeRef {
                source = this@toFirOrImplicitType?.toFirSourceElement(KtFakeSourceElementKind.ImplicitTypeRef)
            }

        private fun KtTypeReference?.toFirOrUnitType(): FirTypeRef =
            convertSafe() ?: implicitUnitType

        private fun KtTypeReference?.toFirOrErrorType(): FirTypeRef =
            convertSafe() ?: buildErrorTypeRef {
                source = this@toFirOrErrorType?.toFirSourceElement()
                diagnostic = ConeSimpleDiagnostic(
                    if (this@toFirOrErrorType == null) "Incomplete code" else "Conversion failed", DiagnosticKind.Syntax
                )
            }

        // Here we accept lambda as receiver to prevent expression calculation in stub mode
        private fun (() -> KtExpression?).toFirExpression(errorReason: String): FirExpression =
            with(this()) {
                convertSafe() ?: buildErrorExpression(
                    this?.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionExpected),
                )
            }

        private fun KtElement?.toFirExpression(
            errorReason: String,
            kind: DiagnosticKind = DiagnosticKind.ExpressionExpected
        ): FirExpression {
            val result = this.convertSafe<FirExpression>()

            if (result != null) {
                if (this == null) {
                    return result
                }

                val callExpressionCallee = (this as? KtCallExpression)?.calleeExpression?.unwrapParenthesesLabelsAndAnnotations()

                if (this is KtNameReferenceExpression ||
                    this is KtConstantExpression ||
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

            return buildErrorExpression(
                this?.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, kind),
            )
        }

        private inline fun KtExpression.toFirStatement(errorReasonLazy: () -> String): FirStatement =
            convertSafe() ?: buildErrorExpression(this.toFirSourceElement(), ConeSimpleDiagnostic(errorReasonLazy(), DiagnosticKind.Syntax))

        private fun KtExpression.toFirStatement(): FirStatement =
            convert()

        private fun KtDeclaration.toFirDeclaration(
            delegatedSuperType: FirTypeRef,
            delegatedSelfType: FirResolvedTypeRef,
            owner: KtClassOrObject,
            ownerClassBuilder: FirClassBuilder,
            ownerTypeParameters: List<FirTypeParameterRef>
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> {
                    disabledLazyMode {
                        toFirConstructor(
                            delegatedSuperType,
                            delegatedSelfType,
                            owner,
                            ownerTypeParameters
                        )
                    }
                }
                is KtEnumEntry -> {
                    val primaryConstructor = owner.primaryConstructor
                    val ownerClassHasDefaultConstructor =
                        primaryConstructor?.valueParameters?.isEmpty() ?: owner.secondaryConstructors.let { constructors ->
                            constructors.isEmpty() || constructors.any { it.valueParameters.isEmpty() }
                        }
                    disabledLazyMode {
                        toFirEnumEntry(delegatedSelfType, ownerClassHasDefaultConstructor)
                    }
                }
                is KtProperty -> {
                    convertProperty(
                        this@toFirDeclaration,
                        ownerClassBuilder.ownerRegularOrAnonymousObjectSymbol,
                        ownerClassBuilder.ownerRegularClassTypeParametersCount
                    )
                }
                else -> convert()
            }
        }

        private fun KtExpression?.toFirBlock(): FirBlock =
            when (this) {
                is KtBlockExpression ->
                    accept(this@Visitor, Unit) as FirBlock
                null ->
                    buildEmptyExpressionBlock()
                else ->
                    FirSingleExpressionBlock(convert())
            }

        private fun KtDeclarationWithBody.buildFirBody(): Pair<FirBlock?, FirContractDescription?> =
            when {
                !hasBody() ->
                    null to null
                mode == BodyBuildingMode.LAZY_BODIES -> {
                    val block = buildLazyBlock {
                        source = bodyExpression?.toFirSourceElement()
                            ?: error("hasBody() == true but body is null")
                    }
                    block to null
                }
                hasBlockBody() -> {
                    val block = bodyBlockExpression?.accept(this@Visitor, Unit) as? FirBlock
                    if (hasContractEffectList()) {
                        block to null
                    } else {
                        block.extractContractDescriptionIfPossible()
                    }
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    FirSingleExpressionBlock(result.toReturn(baseSource = result.source)) to null
                }
            }

        private fun ValueArgument?.toFirExpression(): FirExpression {
            if (this == null) {
                return buildErrorExpression(
                    (this as? KtElement)?.toFirSourceElement(),
                    ConeSimpleDiagnostic("No argument given", DiagnosticKind.Syntax),
                )
            }
            val name = this.getArgumentName()?.asName
            val firExpression = when (val expression = this.getArgumentExpression()) {
                is KtConstantExpression, is KtStringTemplateExpression -> {
                    expression.accept(this@Visitor, Unit) as FirExpression
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
            val accessorVisibility =
                if (this?.visibility != null && this.visibility != Visibilities.Unknown) this.visibility else property.visibility
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
                            returnTypeReference?.convertSafe() ?: propertyTypeRefToUse
                        } else {
                            returnTypeReference.toFirOrUnitType()
                        }
                        this.isGetter = isGetter
                        this.status = status
                        extractAnnotationsTo(this)
                        annotations += accessorAnnotationsFromProperty
                        this@RawFirBuilder.context.firFunctionTargets += accessorTarget
                        extractValueParametersTo(
                            this, ValueParameterDeclaration.OTHER, propertyTypeRefToUse, parameterAnnotationsFromProperty
                        )
                        if (!isGetter && valueParameters.isEmpty()) {
                            valueParameters += buildDefaultSetterValueParameter {
                                this.source = source.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                returnTypeRef = propertyTypeRefToUse
                                symbol = FirValueParameterSymbol(DEFAULT_VALUE_PARAMETER)
                                annotations += parameterAnnotationsFromProperty
                            }
                        }
                        symbol = FirPropertyAccessorSymbol()
                        val outerContractDescription = this@toFirPropertyAccessor.obtainContractDescription()
                        val bodyWithContractDescription = this@toFirPropertyAccessor.buildFirBody()
                        this.body = bodyWithContractDescription.first
                        val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
                        contractDescription?.let {
                            this.contractDescription = it
                        }
                        this.propertySymbol = propertySymbol
                    }.also {
                        it.initContainingClassAttr()
                        bindFunctionTarget(accessorTarget, it)
                        this@RawFirBuilder.context.firFunctionTargets.removeLast()
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
                            it.annotations += accessorAnnotationsFromProperty
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
        ): FirBackingField {
            val componentVisibility = if (this?.visibility != null && this.visibility != Visibilities.Unknown) {
                this.visibility
            } else {
                Visibilities.Private
            }
            val status = obtainPropertyComponentStatus(componentVisibility, this, property)
            val backingFieldInitializer = when {
                this?.hasInitializer() == false -> null
                mode == BodyBuildingMode.LAZY_BODIES -> buildLazyExpression {
                    source = this@toFirBackingField?.initializer?.toFirSourceElement()
                }
                else -> this@toFirBackingField?.initializer.toFirExpression("Should have initializer")
            }
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
                    annotations = mutableListOf(),
                    returnTypeRef = propertyReturnType.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                    isVar = property.isVar,
                    propertySymbol = propertySymbol,
                    status = status,
                )
            }
        }

        private fun KtParameter.toFirValueParameter(
            defaultTypeRef: FirTypeRef?,
            valueParameterDeclaration: ValueParameterDeclaration,
            additionalAnnotations: List<FirAnnotation>
        ): FirValueParameter {
            val name = convertValueParameterName(nameAsSafeName, nameIdentifier?.node?.text, valueParameterDeclaration)
            return buildValueParameter {
                source = toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultTypeRef != null -> defaultTypeRef
                    else -> null.toFirOrImplicitType()
                }
                this.name = name
                symbol = FirValueParameterSymbol(name)
                defaultValue = if (hasDefaultValue()) {
                    disabledLazyMode {
                        // TODO build lazy initializers here
                        { this@toFirValueParameter.defaultValue }.toFirExpression("Should have default value")
                    }
                } else null
                isCrossinline = hasModifier(CROSSINLINE_KEYWORD)
                isNoinline = hasModifier(NOINLINE_KEYWORD)
                isVararg = isVarArg
                val isFromPrimaryConstructor = valueParameterDeclaration == ValueParameterDeclaration.PRIMARY_CONSTRUCTOR
                for (annotationEntry in annotationEntries) {
                    annotationEntry.convert<FirAnnotation>().takeIf {
                        !isFromPrimaryConstructor || it.useSiteTarget == null ||
                                it.useSiteTarget == CONSTRUCTOR_PARAMETER ||
                                it.useSiteTarget == RECEIVER ||
                                it.useSiteTarget == FILE
                    }?.let {
                        this.annotations += it
                    }
                }
                annotations += additionalAnnotations
            }
        }

        private fun KtParameter.toFirProperty(firParameter: FirValueParameter): FirProperty {
            require(hasValOrVar())
            val type = typeReference.toFirOrErrorType()
            val status = FirDeclarationStatusImpl(visibility, modality).apply {
                isExpect = hasExpectModifier() || this@RawFirBuilder.context.containerIsExpect
                isActual = hasActualModifier()
                isOverride = hasModifier(OVERRIDE_KEYWORD)
                isConst = hasModifier(CONST_KEYWORD)
                isLateInit = false
            }
            val propertySource = toFirSourceElement(KtFakeSourceElementKind.PropertyFromParameter)
            val propertyName = nameAsSafeName
            val parameterAnnotations = mutableListOf<FirAnnotationCall>()
            for (annotationEntry in annotationEntries) {
                parameterAnnotations += annotationEntry.convert<FirAnnotationCall>()
            }
            return buildProperty {
                source = propertySource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = type.copyWithNewSourceKind(KtFakeSourceElementKind.PropertyFromParameter)
                receiverTypeRef = null
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
                symbol = FirPropertySymbol(callableIdForName(propertyName))
                isLocal = false
                this.status = status
                val defaultAccessorSource = propertySource.fakeElement(KtFakeSourceElementKind.DefaultAccessor)
                getter = FirDefaultPropertyGetter(
                    defaultAccessorSource,
                    baseModuleData,
                    FirDeclarationOrigin.Source,
                    type.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                    visibility,
                    symbol,
                ).also { getter ->
                    getter.initContainingClassAttr()
                    getter.annotations += parameterAnnotations.filterUseSiteTarget(PROPERTY_GETTER)
                }
                setter = if (isMutable) FirDefaultPropertySetter(
                    defaultAccessorSource,
                    baseModuleData,
                    FirDeclarationOrigin.Source,
                    type.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                    visibility,
                    symbol,
                    parameterAnnotations = parameterAnnotations.filterUseSiteTarget(SETTER_PARAMETER)
                ).also { setter ->
                    setter.initContainingClassAttr()
                    setter.annotations += parameterAnnotations.filterUseSiteTarget(PROPERTY_SETTER)
                } else null
                annotations += parameterAnnotations.filter {
                    it.useSiteTarget == null || it.useSiteTarget == PROPERTY ||
                            it.useSiteTarget == FIELD ||
                            it.useSiteTarget == PROPERTY_DELEGATE_FIELD
                }

                dispatchReceiverType = currentDispatchReceiverType()
            }.apply {
                if (firParameter.isVararg) {
                    isFromVararg = true
                }
                firParameter.correspondingProperty = this
                fromPrimaryConstructor = true
            }
        }

        private fun FirDefaultPropertyAccessor.extractAnnotationsFrom(annotated: KtAnnotated) {
            annotated.extractAnnotationsTo(this.annotations)
        }

        private fun KtAnnotated.extractAnnotationsTo(container: MutableList<FirAnnotation>) {
            for (annotationEntry in annotationEntries) {
                container += annotationEntry.convert<FirAnnotation>()
            }
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAnnotationContainerBuilder) {
            extractAnnotationsTo(container.annotations)
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(
            container: FirTypeParameterRefsOwnerBuilder,
            declarationSymbol: FirBasedSymbol<*>
        ) {
            for (typeParameter in typeParameters) {
                container.typeParameters += extractTypeParameter(typeParameter, declarationSymbol)
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(
            container: FirTypeParametersOwnerBuilder,
            declarationSymbol: FirBasedSymbol<*>
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
                }
                addDefaultBoundIfNecessary()
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit?): FirElement {
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
            valueParameterDeclaration: ValueParameterDeclaration,
            defaultTypeRef: FirTypeRef? = null,
            additionalAnnotations: List<FirAnnotation> = emptyList(),
        ) {
            for (valueParameter in valueParameters) {
                container.valueParameters += convertValueParameter(
                    valueParameter, defaultTypeRef, valueParameterDeclaration, additionalAnnotations = additionalAnnotations
                )
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirCallBuilder) {
            val argumentList = buildArgumentList {
                source = valueArgumentList?.toFirSourceElement()
                for (argument in valueArguments) {
                    val argumentExpression = disabledLazyMode { argument.toFirExpression() }
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

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(
            container: FirClassBuilder,
            delegatedSelfTypeRef: FirTypeRef?,
            delegatedEnumSuperTypeRef: FirTypeRef?,
            classKind: ClassKind,
            containerTypeParameters: List<FirTypeParameterRef>,
            containingClassIsExpectClass: Boolean
        ): Pair<FirTypeRef, Map<Int, FirFieldSymbol>?> {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
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
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        val type = superTypeListEntry.typeReference.toFirOrErrorType()
                        val delegateExpression = { superTypeListEntry.delegateExpression }.toFirExpression("Should have delegate")
                        container.superTypeRefs += type
                        val delegateName = Name.special("<\$\$delegate_${delegateFieldsMap.size}>")
                        val delegateSource =
                            superTypeListEntry.delegateExpression?.toFirSourceElement(KtFakeSourceElementKind.ClassDelegationField)
                        val delegateField = buildField {
                            source = delegateSource
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Synthetic
                            name = delegateName
                            returnTypeRef = type
                            symbol = FirFieldSymbol(CallableId(name))
                            isVar = false
                            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                            initializer = delegateExpression
                        }
                        delegateFieldsMap[index] = delegateField.symbol
                    }
                }
            }

            when {
                this is KtClass && classKind == ClassKind.ENUM_CLASS -> {
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

            val defaultDelegatedSuperTypeRef =
                when {
                    classKind == ClassKind.ENUM_ENTRY && this is KtClass -> delegatedEnumSuperTypeRef ?: implicitAnyType
                    container.superTypeRefs.isEmpty() -> implicitAnyType
                    else -> buildImplicitTypeRef()
                }


            if (container.superTypeRefs.isEmpty()) {
                container.superTypeRefs += implicitAnyType
                delegatedSuperTypeRef = implicitAnyType
            }

            // TODO: in case we have no primary constructor,
            // it may be not possible to determine delegated super type right here
            delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef
            val shouldNotGenerateImplicitPrimaryConstructor = hasExplicitPrimaryConstructor() || hasSecondaryConstructors() ||
                    (containingClassIsExpectClass && classKind != ClassKind.ENUM_CLASS)

            if ((this !is KtClass || !this.isInterface()) && (primaryConstructor != null || !shouldNotGenerateImplicitPrimaryConstructor)) {
                val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                    superTypeCallEntry,
                    delegatedSuperTypeRef!!,
                    delegatedSelfTypeRef ?: delegatedSuperTypeRef!!,
                    owner = this,
                    containerTypeParameters,
                    body = null
                )
                container.declarations += firPrimaryConstructor
            }
            delegateFieldsMap.values.mapTo(container.declarations) { it.fir }
            return delegatedSuperTypeRef!! to delegateFieldsMap.takeIf { it.isNotEmpty() }
        }

        private fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            ownerTypeParameters: List<FirTypeParameterRef>,
            body: FirBlock? = null
        ): FirConstructor {
            val constructorCall = superTypeCallEntry?.toFirSourceElement()
            val constructorSource = this?.toFirSourceElement()
                ?: owner.toKtPsiSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
            val firDelegatedCall = buildDelegatedConstructorCall {
                source = constructorCall ?: constructorSource.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                constructedTypeRef = delegatedSuperTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
                isThis = false
                calleeReference = buildExplicitSuperReference {
                    source = superTypeCallEntry?.calleeExpression?.toFirSourceElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                        ?: this@buildDelegatedConstructorCall.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall)
                    superTypeRef = this@buildDelegatedConstructorCall.constructedTypeRef
                }
                superTypeCallEntry?.extractArgumentsTo(this)
            }

            // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
            fun defaultVisibility() = when {
                owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) || owner is KtEnumEntry -> Visibilities.Private
                owner.hasModifier(SEALED_KEYWORD) -> Visibilities.Protected
                else -> Visibilities.Unknown
            }

            val explicitVisibility = this?.visibility?.takeUnless { it == Visibilities.Unknown }
            val status = FirDeclarationStatusImpl(explicitVisibility ?: defaultVisibility(), Modality.FINAL).apply {
                isExpect = this@toFirConstructor?.hasExpectModifier() == true || this@RawFirBuilder.context.containerIsExpect
                isActual = this@toFirConstructor?.hasActualModifier() == true
                isInner = owner.hasModifier(INNER_KEYWORD)
                isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
            }
            return buildPrimaryConstructor {
                source = constructorSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                this.status = status
                dispatchReceiverType = owner.obtainDispatchReceiverForConstructor()
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = firDelegatedCall
                typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                this@toFirConstructor?.extractAnnotationsTo(this)
                this@toFirConstructor?.extractValueParametersTo(this, ValueParameterDeclaration.PRIMARY_CONSTRUCTOR)
                this.body = body
            }.apply {
                containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
            }
        }

        private fun KtClassOrObject.obtainDispatchReceiverForConstructor(): ConeClassLikeType? =
            if (hasModifier(INNER_KEYWORD)) dispatchReceiverForInnerClassConstructor() else null

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            context.packageFqName = if (psiMode == PsiHandlingMode.COMPILER) file.packageFqNameByTree else file.packageFqName
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
                for (annotationEntry in file.annotationEntries) {
                    annotations += annotationEntry.convert<FirAnnotation>()
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
                for (declaration in file.declarations) {
                    // TODO: scripts aren't supported yet
                    if (declaration is KtScript) continue
                    declarations += declaration.convert<FirDeclaration>()
                }
            }
        }

        private fun KtEnumEntry.toFirEnumEntry(
            delegatedEnumSelfTypeRef: FirResolvedTypeRef,
            ownerClassHasDefaultConstructor: Boolean
        ): FirDeclaration {
            val ktEnumEntry = this@toFirEnumEntry
            return buildEnumEntry {
                source = toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedEnumSelfTypeRef
                name = nameAsSafeName
                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                    isStatic = true
                    isExpect = hasExpectModifier() || this@RawFirBuilder.context.containerIsExpect
                }
                symbol = FirEnumEntrySymbol(callableIdForName(nameAsSafeName))
                if (ownerClassHasDefaultConstructor && ktEnumEntry.initializerList == null &&
                    ktEnumEntry.annotationEntries.isEmpty() && ktEnumEntry.body == null
                ) {
                    return@buildEnumEntry
                }
                extractAnnotationsTo(this)
                initializer = withChildClassName(nameAsSafeName, isExpect = false) {
                    buildAnonymousObjectExpression {
                        val enumEntrySource = toFirSourceElement(KtFakeSourceElementKind.EnumInitializer)
                        source = enumEntrySource
                        anonymousObject = buildAnonymousObject {
                            source = enumEntrySource
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            classKind = ClassKind.ENUM_ENTRY
                            scopeProvider = this@RawFirBuilder.baseScopeProvider
                            symbol = FirAnonymousObjectSymbol()
                            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)

                            val delegatedEntrySelfType = buildResolvedTypeRef {
                                type =
                                    ConeClassLikeTypeImpl(this@buildAnonymousObject.symbol.toLookupTag(), emptyArray(), isNullable = false)
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
                                typeParameters
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
            }.apply {
                containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
            }
        }

        override fun visitClassInitializer(initializer: KtClassInitializer, data: Unit?): FirElement {
            return disabledLazyMode {
                super.visitClassInitializer(initializer, data)
            }
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit): FirElement {
            // NB: enum entry nested classes are considered local by FIR design (see discussion in KT-45115)
            val isLocal = classOrObject.isLocal || classOrObject.getStrictParentOfType<KtEnumEntry>() != null
            val classIsExpect = classOrObject.hasExpectModifier() || context.containerIsExpect
            return withChildClassName(
                classOrObject.nameAsSafeName,
                isExpect = classIsExpect,
                forceLocalContext = isLocal
            ) {
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
                    if (isLocal) Visibilities.Local else classOrObject.visibility,
                    classOrObject.modality,
                ).apply {
                    isExpect = classIsExpect
                    isActual = classOrObject.hasActualModifier()
                    isInner = classOrObject.hasModifier(INNER_KEYWORD)
                    isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
                    isData = classOrObject.hasModifier(DATA_KEYWORD)
                    isInline = classOrObject.hasModifier(INLINE_KEYWORD) || classOrObject.hasModifier(VALUE_KEYWORD)
                    isFun = classOrObject.hasModifier(FUN_KEYWORD)
                    isExternal = classOrObject.hasModifier(EXTERNAL_KEYWORD)
                }

                withCapturedTypeParameters(status.isInner || isLocal, classOrObject.toFirSourceElement(), listOf()) {
                    var delegatedFieldsMap: Map<Int, FirFieldSymbol>?
                    buildRegularClass {
                        source = classOrObject.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = classOrObject.nameAsSafeName
                        this.status = status
                        this.classKind = classKind
                        scopeProvider = baseScopeProvider
                        symbol = FirRegularClassSymbol(context.currentClassId)

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
                                ),
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
                                    // just making a shallow copy isn't enough type ref may be a function type ref
                                    // and contain value parameters inside
                                    withDefaultSourceElementKind(newKind) {
                                        (property.returnTypeRef.psi as KtTypeReference?).toFirOrImplicitType()
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
                        }

                        initCompanionObjectSymbolAttr()

                        context.popFirTypeParameters()
                    }.also {
                        it.delegateFieldsMap = delegatedFieldsMap
                    }
                }
            }.also {
                it.initContainingClassForLocalAttr()
                classOrObject.fillDanglingConstraintsTo(it)
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return withChildClassName(SpecialNames.ANONYMOUS, forceLocalContext = true, isExpect = false) {
                var delegatedFieldsMap: Map<Int, FirFieldSymbol>?
                buildAnonymousObjectExpression {
                    val sourceElement = objectDeclaration.toFirSourceElement()
                    source = sourceElement
                    anonymousObject = buildAnonymousObject {
                        source = sourceElement
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        classKind = ClassKind.OBJECT
                        scopeProvider = baseScopeProvider
                        symbol = FirAnonymousObjectSymbol()
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
                    }.also {
                        it.delegateFieldsMap = delegatedFieldsMap
                    }
                }
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit): FirElement {
            val typeAliasIsExpect = typeAlias.hasExpectModifier() || context.containerIsExpect
            return withChildClassName(typeAlias.nameAsSafeName, isExpect = typeAliasIsExpect) {
                buildTypeAlias {
                    source = typeAlias.toFirSourceElement()
                    moduleData = baseModuleData
                    origin = FirDeclarationOrigin.Source
                    name = typeAlias.nameAsSafeName
                    status = FirDeclarationStatusImpl(typeAlias.visibility, Modality.FINAL).apply {
                        isExpect = typeAliasIsExpect
                        isActual = typeAlias.hasActualModifier()
                    }
                    symbol = FirTypeAliasSymbol(context.currentClassId)
                    expandedTypeRef = typeAlias.getTypeReference().toFirOrErrorType()
                    typeAlias.extractAnnotationsTo(this)
                    typeAlias.extractTypeParametersTo(this, symbol)
                }
            }
        }

        override fun visitNamedFunction(function: KtNamedFunction, data: Unit): FirElement {
            val typeReference = function.typeReference
            val returnType = if (function.hasBlockBody()) {
                typeReference.toFirOrUnitType()
            } else {
                typeReference.toFirOrImplicitType()
            }
            val receiverType = function.receiverTypeReference.convertSafe<FirTypeRef>()

            val labelName: String?
            val isAnonymousFunction = function.name == null && !function.parent.let { it is KtFile || it is KtClassBody }
            val functionBuilder = if (isAnonymousFunction) {
                FirAnonymousFunctionBuilder().apply {
                    receiverTypeRef = receiverType
                    symbol = FirAnonymousFunctionSymbol()
                    isLambda = false
                    hasExplicitParameterList = true
                    label = context.getLastLabel(function)
                    labelName = label?.name ?: context.calleeNamesForLambda.lastOrNull()?.identifier
                }
            } else {
                FirSimpleFunctionBuilder().apply {
                    receiverTypeRef = receiverType
                    name = function.nameAsSafeName
                    labelName = runIf(!name.isSpecial) { name.identifier }
                    symbol = FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName))
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = FirDeclarationStatusImpl(
                        if (function.isLocal) Visibilities.Local else function.visibility,
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
                    valueParameters += convertValueParameter(
                        valueParameter,
                        null,
                        if (isAnonymousFunction) ValueParameterDeclaration.LAMBDA else ValueParameterDeclaration.OTHER
                    )
                }
                val actualTypeParameters = if (this is FirSimpleFunctionBuilder)
                    this.typeParameters
                else
                    listOf()
                withCapturedTypeParameters(true, functionSource, actualTypeParameters) {
                    val outerContractDescription = function.obtainContractDescription()
                    val bodyWithContractDescription = function.buildFirBody()
                    this.body = bodyWithContractDescription.first
                    val contractDescription = outerContractDescription ?: bodyWithContractDescription.second
                    contractDescription?.let {
                        // TODO: add error reporting for contracts on lambdas
                        if (this is FirSimpleFunctionBuilder) {
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
            getExpressions()
                .mapTo(destination) { it.accept(this@Visitor, Unit) as FirExpression }
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit): FirElement {
            val literal = expression.functionLiteral
            val literalSource = literal.toFirSourceElement()
            val implicitTypeRefSource = literal.toFirSourceElement(KtFakeSourceElementKind.ImplicitTypeRef)
            val returnType = buildImplicitTypeRef {
                source = implicitTypeRefSource
            }
            val receiverType = buildImplicitTypeRef {
                source = implicitTypeRefSource
            }

            val target: FirFunctionTarget
            val anonymousFunction = buildAnonymousFunction {
                source = literalSource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                receiverTypeRef = receiverType
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true
                hasExplicitParameterList = expression.functionLiteral.arrow != null

                val destructuringStatements = mutableListOf<FirStatement>()
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val name = SpecialNames.DESTRUCT
                        val multiParameter = buildValueParameter {
                            source = valueParameter.toFirSourceElement()
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = valueParameter.typeReference?.convertSafe() ?: buildImplicitTypeRef {
                                source = multiDeclaration.toFirSourceElement(KtFakeSourceElementKind.ImplicitTypeRef)
                            }
                            this.name = name
                            symbol = FirValueParameterSymbol(name)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                        destructuringStatements += generateDestructuringBlock(
                            baseModuleData,
                            multiDeclaration,
                            multiParameter,
                            tmpVariable = false,
                            extractAnnotationsTo = { extractAnnotationsTo(it) },
                        ) { toFirOrImplicitType() }.statements
                        multiParameter
                    } else {
                        val typeRef = valueParameter.typeReference?.convertSafe() ?: buildImplicitTypeRef {
                            source = implicitTypeRefSource
                        }
                        convertValueParameter(valueParameter, typeRef, ValueParameterDeclaration.LAMBDA)
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
                body = if (ktBody == null) {
                    val errorExpression = buildErrorExpression(source, ConeSimpleDiagnostic("Lambda has no body", DiagnosticKind.Syntax))
                    FirSingleExpressionBlock(errorExpression.toReturn())
                } else {
                    configureBlockWithoutBuilding(ktBody).apply {
                        if (statements.isEmpty()) {
                            statements.add(
                                buildReturnExpression {
                                    source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitReturn)
                                    this.target = target
                                    result = buildUnitExpression {
                                        source = expressionSource.fakeElement(KtFakeSourceElementKind.ImplicitUnit)
                                    }
                                }
                            )
                        }
                        statements.addAll(0, destructuringStatements)
                    }.build()
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

        private fun KtSecondaryConstructor.toFirConstructor(
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            ownerTypeParameters: List<FirTypeParameterRef>
        ): FirConstructor {
            val target = FirFunctionTarget(labelName = null, isLambda = false)
            return buildConstructor {
                source = this@toFirConstructor.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                val explicitVisibility = visibility
                status = FirDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
                    isExpect = hasExpectModifier() || this@RawFirBuilder.context.containerIsExpect
                    isActual = hasActualModifier()
                    isInner = owner.hasModifier(INNER_KEYWORD)
                    isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                    isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                }
                dispatchReceiverType = owner.obtainDispatchReceiverForConstructor()
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = getDelegationCall().convert(
                    delegatedSuperTypeRef,
                    delegatedSelfTypeRef,
                )
                this@RawFirBuilder.context.firFunctionTargets += target
                extractAnnotationsTo(this)
                typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                extractValueParametersTo(this, ValueParameterDeclaration.OTHER)


                val (body, _) = buildFirBody()
                this.body = body
                this@RawFirBuilder.context.firFunctionTargets.removeLast()
            }.also {
                it.containingClassForStaticMemberAttr = currentDispatchReceiverType()!!.lookupTag
                bindFunctionTarget(target, it)
            }
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis //|| (isImplicit && hasPrimaryConstructor)
            val source = if (isImplicit) {
                this.toFirSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
            } else {
                this.toFirSourceElement()
            }
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef
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

        private fun <T> KtProperty.toFirProperty(
            ownerRegularOrAnonymousObjectSymbol: FirClassSymbol<*>?,
            ownerRegularClassTypeParametersCount: Int?,
            context: Context<T>
        ): FirProperty {
            val propertyType = typeReference.toFirOrImplicitType()
            val propertyName = nameAsSafeName
            val isVar = isVar
            val propertyInitializer = when {
                !hasInitializer() -> null
                mode == BodyBuildingMode.LAZY_BODIES -> buildLazyExpression {
                    source = initializer?.toFirSourceElement()
                }
                else -> initializer.toFirExpression("Should have initializer")

            }

            val propertySource = toFirSourceElement()

            return buildProperty {
                source = propertySource
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = propertyType
                name = propertyName
                this.isVar = isVar

                initializer = propertyInitializer

                val propertyAnnotations = mutableListOf<FirAnnotationCall>()
                for (annotationEntry in annotationEntries) {
                    propertyAnnotations += annotationEntry.convert<FirAnnotationCall>()
                }
                if (this@toFirProperty.isLocal) {
                    isLocal = true
                    symbol = FirPropertySymbol(propertyName)

                    backingField = this@toFirProperty.fieldDeclaration.toFirBackingField(
                        this@toFirProperty,
                        propertySymbol = symbol,
                        propertyType,
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
                            expression = delegateFirExpression
                        }

                        generateAccessorsByDelegate(
                            delegateBuilder,
                            baseModuleData,
                            ownerRegularOrAnonymousObjectSymbol = null,
                            ownerRegularClassTypeParametersCount = null,
                            isExtension = false,
                            context = context,
                        )
                    }
                } else {
                    isLocal = false
                    receiverTypeRef = receiverTypeReference.convertSafe()
                    symbol = FirPropertySymbol(callableIdForName(propertyName))
                    dispatchReceiverType = currentDispatchReceiverType()
                    extractTypeParametersTo(this, symbol)
                    withCapturedTypeParameters(true, propertySource, this.typeParameters) {
                        backingField = this@toFirProperty.fieldDeclaration.toFirBackingField(
                            this@toFirProperty,
                            propertySymbol = symbol,
                            propertyType,
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

                        status = FirDeclarationStatusImpl(visibility, modality).apply {
                            isExpect = hasExpectModifier() || this@RawFirBuilder.context.containerIsExpect
                            isActual = hasActualModifier()
                            isOverride = hasModifier(OVERRIDE_KEYWORD)
                            isConst = hasModifier(CONST_KEYWORD)
                            isLateInit = hasModifier(LATEINIT_KEYWORD)
                            isExternal = hasModifier(EXTERNAL_KEYWORD)
                        }

                        if (hasDelegate()) {
                            fun extractDelegateExpression() = when (mode) {
                                BodyBuildingMode.NORMAL -> this@toFirProperty.delegate?.expression.toFirExpression("Should have delegate")
                                BodyBuildingMode.LAZY_BODIES -> this@toFirProperty.delegate?.expression?.let {
                                    buildLazyExpression { source = it.toFirSourceElement() }
                                } ?: buildErrorExpression {
                                    ConeSimpleDiagnostic("Should have delegate", DiagnosticKind.ExpressionExpected)
                                }
                            }

                            val delegateBuilder = FirWrappedDelegateExpressionBuilder().apply {
                                val delegateExpression = extractDelegateExpression()
                                source = delegateExpression.source?.fakeElement(KtFakeSourceElementKind.WrappedDelegate)
                                expression = extractDelegateExpression()
                            }

                            generateAccessorsByDelegate(
                                delegateBuilder,
                                baseModuleData,
                                ownerRegularOrAnonymousObjectSymbol,
                                ownerRegularClassTypeParametersCount,
                                context,
                                isExtension = receiverTypeReference != null,
                            )
                        }
                    }
                }
                annotations += if (isLocal) propertyAnnotations else propertyAnnotations.filter {
                    it.useSiteTarget != PROPERTY_GETTER &&
                            (!isVar || it.useSiteTarget != SETTER_PARAMETER && it.useSiteTarget != PROPERTY_SETTER)
                }
            }.also {
                if (!isLocal) {
                    fillDanglingConstraintsTo(it)
                }
            }
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return buildAnonymousInitializer {
                source = initializer.toFirSourceElement()
                moduleData = baseModuleData
                origin = FirDeclarationOrigin.Source
                body = initializer.body.toFirBlock()
            }
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            return property.toFirProperty(
                ownerRegularOrAnonymousObjectSymbol = null,
                ownerRegularClassTypeParametersCount = null,
                context = context
            )
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val source = typeReference.toFirSourceElement()
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

            val allModifierLists = mutableListOf<KtModifierList>(*typeReference.getAllModifierLists())

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
                    var referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        FirUserTypeRefBuilder().apply {
                            this.source = source
                            isMarkedNullable = isNullable
                            var ktQualifier: KtUserType? = unwrappedElement

                            do {
                                val firQualifier = FirQualifierPartImpl(
                                    referenceExpression!!.toFirSourceElement(),
                                    referenceExpression!!.getReferencedNameAsName(),
                                    FirTypeArgumentListImpl(ktQualifier?.typeArgumentList?.toKtPsiSourceElement() ?: source).apply {
                                        for (typeArgument in ktQualifier!!.typeArguments) {
                                            typeArguments += typeArgument.convert<FirTypeProjection>()
                                        }
                                    }
                                )
                                qualifier.add(firQualifier)

                                ktQualifier = ktQualifier!!.qualifier
                                referenceExpression = ktQualifier?.referenceExpression
                            } while (referenceExpression != null)

                            qualifier.reverse()
                        }
                    } else {
                        FirErrorTypeRefBuilder().apply {
                            this.source = source
                            diagnostic = ConeSimpleDiagnostic("Incomplete user type", DiagnosticKind.Syntax)
                        }
                    }
                }
                is KtFunctionType -> {
                    FirFunctionTypeRefBuilder().apply {
                        this.source = source
                        isMarkedNullable = isNullable
                        isSuspend = allModifierLists.any { it.hasSuspendModifier() }
                        receiverTypeRef = unwrappedElement.receiverTypeReference.convertSafe()
                        // TODO: probably implicit type should not be here
                        returnTypeRef = unwrappedElement.returnTypeReference.toFirOrErrorType()
                        for (valueParameter in unwrappedElement.parameters) {
                            valueParameters += valueParameter.convert<FirValueParameter>()
                        }
                        if (receiverTypeRef != null) {
                            annotations += extensionFunctionAnnotation
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
                    diagnostic = ConeSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax)
                }
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (modifierList in allModifierLists) {
                for (annotationEntry in modifierList.annotationEntries) {
                    firTypeBuilder.annotations += annotationEntry.convert<FirAnnotation>()
                }
            }
            return firTypeBuilder.build()
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit): FirElement {
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
            }
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit): FirElement {
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
                    KtProjectionKind.STAR -> throw AssertionError("* should not be here")
                }
            }
        }

        override fun visitParameter(parameter: KtParameter, data: Unit): FirElement =
            convertValueParameter(parameter)

        override fun visitBlockExpression(expression: KtBlockExpression, data: Unit): FirElement {
            return configureBlockWithoutBuilding(expression).build()
        }

        private fun configureBlockWithoutBuilding(expression: KtBlockExpression): FirBlockBuilder {
            return FirBlockBuilder().apply {
                source = expression.toFirSourceElement()
                for (statement in expression.statements) {
                    val firStatement = statement.toFirStatement { "Statement expected: ${statement.text}" }
                    val isForLoopBlock = firStatement is FirBlock && firStatement.source?.kind == KtFakeSourceElementKind.DesugaredForLoop
                    if (firStatement !is FirBlock || isForLoopBlock || firStatement.annotations.isNotEmpty()) {
                        statements += firStatement
                    } else {
                        statements += firStatement.statements
                    }
                }
            }
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit): FirElement {
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

        override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): FirElement =
            generateConstantExpressionByLiteral(expression)

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Unit): FirElement {
            return expression.entries.toInterpolatingCall(
                expression,
                getElementType = { element ->
                    when (element) {
                        is KtLiteralStringTemplateEntry -> KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY
                        is KtEscapeStringTemplateEntry -> KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY
                        is KtSimpleNameStringTemplateEntry -> KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY
                        is KtBlockStringTemplateEntry -> KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY
                        else -> error("invalid node type $element")
                    }
                },
                convertTemplateEntry = {
                    (this as KtStringTemplateEntryWithExpression).expression.toFirExpression(it)
                },
            )
        }

        override fun visitReturnExpression(expression: KtReturnExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement(KtFakeSourceElementKind.ImplicitUnit)
            val result = expression.returnedExpression?.toFirExpression("Incorrect return expression")
                ?: buildUnitExpression { this.source = source }
            return result.toReturn(source, expression.getTargetLabel()?.getReferencedName(), fromKtReturnExpression = true)
        }

        override fun visitTryExpression(expression: KtTryExpression, data: Unit): FirElement {
            return buildTryExpression {
                source = expression.toFirSourceElement()
                tryBlock = expression.tryBlock.toFirBlock()
                finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.let {
                        convertValueParameter(
                            it,
                            valueParameterDeclaration = ValueParameterDeclaration.CATCH
                        )
                    } ?: continue
                    catches += buildCatch {
                        source = clause.toFirSourceElement()
                        this.parameter = parameter
                        block = clause.catchBody.toFirBlock()
                    }
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression, data: Unit): FirElement {
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

        override fun visitWhenExpression(expression: KtWhenExpression, data: Unit): FirElement {
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
                        receiverTypeRef = null
                        this.name = name
                        initializer = subjectExpression
                        delegate = null
                        isVar = false
                        symbol = FirPropertySymbol(name)
                        isLocal = true
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    }
                }
                else -> null
            }
            val hasSubject = subjectExpression != null

            @OptIn(FirContractViolation::class)
            val ref = FirExpressionRef<FirWhenExpression>()
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
                                condition = ((ktCondition as? KtWhenConditionWithExpression)?.expression
                                    ?: ktCondition)
                                    .toFirExpression(
                                        "No expression in condition with expression",
                                        DiagnosticKind.ExpressionExpected
                                    )
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
                if (hasSubject) {
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
                        return (parent.parent as? KtExpression)?.usedAsExpression ?: true
                    }
                }
                // Here we check that when used is a single statement of a loop
                if (parent !is KtContainerNodeForControlStructureBody) return true
                val type = parent.parent.elementType
                return !(type == KtNodeTypes.FOR || type == KtNodeTypes.WHILE || type == KtNodeTypes.DO_WHILE)
            }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Unit): FirElement {
            val target: FirLoopTarget
            return FirDoWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                // For break/continue in the do-while loop condition, prepare the loop target first so that it can refer to the same loop.
                target = prepareTarget(expression)
                condition = expression.condition.toFirExpression("No condition in do-while loop")
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: Unit): FirElement {
            val target: FirLoopTarget
            return FirWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                condition = expression.condition.toFirExpression("No condition in while loop")
                // break/continue in the while loop condition will refer to an outer loop if any.
                // So, prepare the loop target after building the condition.
                target = prepareTarget(expression)
            }.configure(target) { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: Unit?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val ktParameter = expression.loopParameter
            val fakeSource = expression.toKtPsiSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
            val target: FirLoopTarget
            // NB: FirForLoopChecker relies on this block existence and structure
            return buildBlock {
                source = fakeSource
                val rangeSource = expression.loopRange?.toFirSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
                val iteratorVal = generateTemporaryVariable(
                    baseModuleData, rangeSource, SpecialNames.ITERATOR,
                    buildFunctionCall {
                        source = fakeSource
                        calleeReference = buildSimpleNamedReference {
                            source = fakeSource
                            name = OperatorNameConventions.ITERATOR
                        }
                        explicitReceiver = rangeExpression
                    },
                )
                statements += iteratorVal
                statements += FirWhileLoopBuilder().apply {
                    source = expression.toFirSourceElement()
                    condition = buildFunctionCall {
                        source = fakeSource
                        calleeReference = buildSimpleNamedReference {
                            source = fakeSource
                            name = OperatorNameConventions.HAS_NEXT
                        }
                        explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                    }
                    // break/continue in the for loop condition will refer to an outer loop if any.
                    // So, prepare the loop target after building the condition.
                    target = prepareTarget(expression)
                }.configure(target) {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val blockBuilder = when (val body = expression.body) {
                        is KtBlockExpression -> configureBlockWithoutBuilding(body)
                        null -> FirBlockBuilder()
                        else -> FirBlockBuilder().apply {
                            source = body.toFirSourceElement(KtFakeSourceElementKind.DesugaredForLoop)
                            statements += body.toFirStatement()
                        }
                    }
                    if (ktParameter != null) {
                        val multiDeclaration = ktParameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            moduleData = baseModuleData, source = expression.loopParameter?.toFirSourceElement(),
                            name = if (multiDeclaration != null) SpecialNames.DESTRUCT else ktParameter.nameAsSafeName,
                            initializer = buildFunctionCall {
                                source = fakeSource
                                calleeReference = buildSimpleNamedReference {
                                    source = fakeSource
                                    name = OperatorNameConventions.NEXT
                                }
                                explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                            },
                            typeRef = ktParameter.typeReference.toFirOrImplicitType(),
                        )
                        if (multiDeclaration != null) {
                            val destructuringBlock = generateDestructuringBlock(
                                baseModuleData,
                                multiDeclaration = multiDeclaration,
                                container = firLoopParameter,
                                tmpVariable = true,
                                extractAnnotationsTo = { extractAnnotationsTo(it) },
                            ) { toFirOrImplicitType() }
                            blockBuilder.statements.addAll(0, destructuringBlock.statements)
                        } else {
                            blockBuilder.statements.add(0, firLoopParameter)
                        }
                    }
                    blockBuilder.build()
                }
            }
        }

        override fun visitBreakExpression(expression: KtBreakExpression, data: Unit): FirElement {
            return FirBreakExpressionBuilder().apply {
                source = expression.toFirSourceElement()
            }.bindLabel(expression).build()
        }

        override fun visitContinueExpression(expression: KtContinueExpression, data: Unit): FirElement {
            return FirContinueExpressionBuilder().apply {
                source = expression.toFirSourceElement()
            }.bindLabel(expression).build()
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken

            if (operationToken == IDENTIFIER) {
                context.calleeNamesForLambda += expression.operationReference.getReferencedNameAsName()
            }

            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")

            if (operationToken == IDENTIFIER) {
                // No need for the callee name since arguments are already generated
                context.calleeNamesForLambda.removeLast()
            }

            val source = expression.toFirSourceElement()

            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(rightArgument, source)
                ANDAND, OROR ->
                    return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND, source)
                in OperatorConventions.IN_OPERATIONS ->
                    return rightArgument.generateContainsOperation(
                        leftArgument, operationToken == NOT_IN, source, expression.operationReference.toFirSourceElement(),
                    )
                in OperatorConventions.COMPARISON_OPERATIONS ->
                    return leftArgument.generateComparisonExpression(
                        rightArgument, operationToken, source, expression.operationReference.toFirSourceElement(),
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
                        (this as KtExpression).toFirExpression("Incorrect expression in assignment: ${expression.text}")
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

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Unit): FirElement {
            return buildTypeOperatorCall {
                source = expression.toFirSourceElement()
                operation = expression.operationReference.getReferencedNameElementType().toFirOperation()
                conversionTypeRef = expression.right.toFirOrErrorType()
                argumentList = buildUnaryArgumentList(expression.left.toFirExpression("No left operand"))
            }
        }

        override fun visitIsExpression(expression: KtIsExpression, data: Unit): FirElement {
            return buildTypeOperatorCall {
                source = expression.toFirSourceElement()
                operation = if (expression.isNegated) FirOperation.NOT_IS else FirOperation.IS
                conversionTypeRef = expression.typeReference.toFirOrErrorType()
                argumentList = buildUnaryArgumentList(expression.leftHandSide.toFirExpression("No left operand"))
            }
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): FirElement {
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
            return when (calleeExpression) {
                is KtSimpleNameExpression ->
                    CalleeAndReceiver(
                        buildSimpleNamedReference {
                            source = calleeExpression.toFirSourceElement()
                            name = calleeExpression.getReferencedNameAsName()
                        }
                    )

                is KtParenthesizedExpression -> splitToCalleeAndReceiver(calleeExpression.expression, defaultSource)

                null -> {
                    CalleeAndReceiver(
                        buildErrorNamedReference { diagnostic = ConeSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax) }
                    )
                }

                is KtSuperExpression -> {
                    CalleeAndReceiver(
                        buildErrorNamedReference {
                            source = calleeExpression.toFirSourceElement()
                            diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed)
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

        override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement()
            val (calleeReference, explicitReceiver, isImplicitInvoke) = splitToCalleeAndReceiver(expression.calleeExpression, source)

            val result: FirQualifiedAccessBuilder = if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
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
                for (typeArgument in expression.typeArguments) {
                    typeArguments += typeArgument.convert<FirTypeProjection>()
                }
            }.build()
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Unit): FirElement {
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

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Unit): FirElement {
            val selector = expression.selectorExpression
                ?: return buildErrorExpression(
                    expression.toFirSourceElement(), ConeSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax),
                )
            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirQualifiedAccess) {
                val receiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")

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
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            return buildThisReceiverExpression {
                val sourceElement = expression.toFirSourceElement()
                source = sourceElement
                calleeReference = buildExplicitThisReference {
                    source = sourceElement.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                    labelName = expression.getLabelName()
                }
            }
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: Unit): FirElement {
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

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): FirElement {
            context.forwardLabelUsagePermission(expression, expression.expression)
            return expression.expression?.accept(this, data)
                ?: buildErrorExpression(expression.toFirSourceElement(), ConeSimpleDiagnostic("Empty parentheses", DiagnosticKind.Syntax))
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): FirElement {
            val label = expression.getTargetLabel()
            var errorLabelSource: KtSourceElement? = null

            val result = if (label != null) {
                val rawName = label.getReferencedNameElement().node!!.text
                val labelAndErrorSource = buildLabelAndErrorSource(rawName, label.toKtPsiSourceElement())
                errorLabelSource = labelAndErrorSource.second
                context.withNewLabel(labelAndErrorSource.first, expression.baseExpression) {
                    expression.baseExpression?.accept(this, data)
                }
            } else {
                expression.baseExpression?.accept(this, data)
            }

            return buildExpressionWithErrorLabel(result, errorLabelSource, expression.toFirSourceElement())
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Unit): FirElement {
            val baseExpression = expression.baseExpression
            context.forwardLabelUsagePermission(expression, baseExpression)
            val rawResult = baseExpression?.accept(this, data)
            val result = rawResult as? FirAnnotationContainer
                ?: buildErrorExpression(
                    expression.toFirSourceElement(),
                    ConeNotAnnotationContainer(rawResult?.render() ?: "???")
                )
            expression.extractAnnotationsTo(result.annotations as MutableList<FirAnnotation>)
            return result
        }

        override fun visitThrowExpression(expression: KtThrowExpression, data: Unit): FirElement {
            return buildThrowExpression {
                source = expression.toFirSourceElement()
                exception = expression.thrownExpression.toFirExpression("Nothing to throw")
            }
        }

        override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Unit): FirElement {
            val baseVariable = generateTemporaryVariable(
                baseModuleData,
                multiDeclaration.toFirSourceElement(),
                "destruct",
                multiDeclaration.initializer.toFirExpression("Initializer required for destructuring declaration", DiagnosticKind.Syntax),
                extractAnnotationsTo = { extractAnnotationsTo(it) }
            )
            return generateDestructuringBlock(
                baseModuleData,
                multiDeclaration,
                baseVariable,
                tmpVariable = true,
                extractAnnotationsTo = { extractAnnotationsTo(it) },
            ) {
                toFirOrImplicitType()
            }
        }

        override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Unit): FirElement {
            return buildGetClassCall {
                source = expression.toFirSourceElement()
                argumentList = buildUnaryArgumentList(expression.receiverExpression.toFirExpression("No receiver in class literal"))
            }
        }

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Unit): FirElement {
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

        override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Unit): FirElement {
            return buildArrayOfCall {
                source = expression.toFirSourceElement()
                argumentList = buildArgumentList {
                    for (innerExpression in expression.getInnerExpressions()) {
                        arguments += innerExpression.toFirExpression("Incorrect collection literal argument")
                    }
                }
            }
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return buildExpressionStub {
                source = expression.toFirSourceElement()
            }
        }
    }

    private val extensionFunctionAnnotation = buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(EXTENSION_FUNCTION_ANNOTATION),
                emptyArray(),
                isNullable = false
            )
        }
        argumentMapping = FirEmptyAnnotationArgumentMapping
    }
}

enum class BodyBuildingMode {
    /**
     * Build every expression and every body
     */
    NORMAL,

    /**
     * Build [org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock] for function bodies, constructors & getters/setters
     * Build [org.jetbrains.kotlin.fir.expressions.impl.FirLazyExpression] for property initializers
     */
    LAZY_BODIES;

    companion object {
        fun lazyBodies(lazyBodies: Boolean): BodyBuildingMode =
            if (lazyBodies) LAZY_BODIES else NORMAL
    }
}

enum class PsiHandlingMode {
    /**
     * Do not build any stubs while handling PSI
     */
    COMPILER,

    /**
     * Build stubs if possible while handling PSI
     */
    IDE;
}
