/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirModifiableQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.util.*

class RawFirBuilder(
    session: FirSession, val baseScopeProvider: FirScopeProvider, val stubMode: Boolean
) : BaseFirBuilder<PsiElement>(session) {

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

    fun buildTypeReference(reference: KtTypeReference): FirTypeRef {
        return reference.accept(Visitor(), Unit) as FirTypeRef
    }

    fun buildFunctionWithBody(function: KtNamedFunction): FirFunction<*> {
        assert(!stubMode) { "Building FIR function with body isn't supported in stub mode" }
        val parentsUpToFile = function.parents
        for (parent in parentsUpToFile.toList().asReversed()) {
            when (parent) {
                is KtFile -> {
                    context.packageFqName = parent.packageFqName
                }
                is KtClassOrObject -> {
                    context.className = context.className.child(parent.nameAsSafeName)
                    context.localBits.add(parent.isLocal || parent.getStrictParentOfType<KtEnumEntry>() != null)
                }
            }
        }
        return function.accept(Visitor(), Unit) as FirFunction<*>
    }

    override fun PsiElement.toFirSourceElement(kind: FirFakeSourceElementKind?): FirPsiSourceElement<*> {
        val actualKind = kind ?: this@RawFirBuilder.context.forcedElementSourceKind ?: FirRealSourceElementKind
        return this.toFirPsiSourceElement(actualKind)
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
        return (this as? KtExpressionWithLabel)?.getLabelName()
    }

    override fun PsiElement.getExpressionInParentheses(): PsiElement? {
        return (this as KtParenthesizedExpression).expression
    }

    override fun PsiElement.getAnnotatedExpression(): PsiElement? {
        return (this as KtAnnotatedExpression).baseExpression
    }

    override val PsiElement?.selectorExpression: PsiElement?
        get() = (this as? KtQualifiedExpression)?.selectorExpression

    private val KtModifierListOwner.visibility: Visibility
        get() = with(modifierList) {
            when {
                this == null -> Visibilities.UNKNOWN
                hasModifier(PRIVATE_KEYWORD) -> Visibilities.PRIVATE
                hasModifier(PUBLIC_KEYWORD) -> Visibilities.PUBLIC
                hasModifier(PROTECTED_KEYWORD) -> Visibilities.PROTECTED
                else -> if (hasModifier(INTERNAL_KEYWORD)) Visibilities.INTERNAL else Visibilities.UNKNOWN
            }
        }

    private val KtDeclaration.modality: Modality?
        get() = with(modifierList) {
            when {
                this == null -> null
                hasModifier(FINAL_KEYWORD) -> Modality.FINAL
                hasModifier(SEALED_KEYWORD) -> Modality.SEALED
                hasModifier(ABSTRACT_KEYWORD) -> Modality.ABSTRACT
                else -> if (hasModifier(OPEN_KEYWORD)) Modality.OPEN else null
            }
        }

    private inner class Visitor : KtVisitor<FirElement, Unit>() {
        private inline fun <reified R : FirElement> KtElement?.convertSafe(): R? =
            this?.accept(this@Visitor, Unit) as? R

        private inline fun <reified R : FirElement> KtElement.convert(): R =
            this.accept(this@Visitor, Unit) as R

        private fun KtTypeReference?.toFirOrImplicitType(): FirTypeRef =
            convertSafe() ?: buildImplicitTypeRef {
                source = this@toFirOrImplicitType?.toFirSourceElement(FirFakeSourceElementKind.ImplicitTypeRef)
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
            if (stubMode) buildExpressionStub()
            else with(this()) {
                convertSafe() ?: buildErrorExpression(
                    this?.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionRequired),
                )
            }

        private fun KtExpression?.toFirExpression(errorReason: String): FirExpression =
            if (stubMode) buildExpressionStub()
            else convertSafe() ?: buildErrorExpression(
                this?.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.ExpressionRequired),
            )

        private fun KtExpression.toFirStatement(errorReason: String): FirStatement =
            convertSafe() ?: buildErrorExpression(this.toFirSourceElement(), ConeSimpleDiagnostic(errorReason, DiagnosticKind.Syntax))

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
                    toFirConstructor(
                        delegatedSuperType,
                        delegatedSelfType,
                        owner,
                        ownerTypeParameters
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
                    toFirProperty(ownerClassBuilder)
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
                hasBlockBody() -> if (!stubMode) {
                    val block = bodyBlockExpression?.accept(this@Visitor, Unit) as? FirBlock
                    block.extractContractDescriptionIfPossible()
                } else {
                    FirSingleExpressionBlock(buildExpressionStub { source = this@buildFirBody.toFirSourceElement() }.toReturn()) to null
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    // basePsi is null, because 'return' is synthetic & should not be bound to some PSI
                    FirSingleExpressionBlock(result.toReturn(baseSource = null)) to null
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
            val expression = this.getArgumentExpression()
            val firExpression = when (expression) {
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
            isGetter: Boolean,
        ): FirPropertyAccessor {
            val accessorVisibility =
                if (this?.visibility != null && this.visibility != Visibilities.UNKNOWN) this.visibility else property.visibility
            // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
            val status =
                FirDeclarationStatusImpl(accessorVisibility, Modality.FINAL).apply {
                    isInline = property.hasModifier(INLINE_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(INLINE_KEYWORD) == true
                    isExternal = property.hasModifier(EXTERNAL_KEYWORD) ||
                            this@toFirPropertyAccessor?.hasModifier(EXTERNAL_KEYWORD) == true
                }
            if (this == null || !hasBody()) {
                val propertySource =
                    if (this != null && hasBody()) property.toFirSourceElement()
                    else property.toFirPsiSourceElement(FirFakeSourceElementKind.DefaultAccessor)
                return FirDefaultPropertyAccessor
                    .createGetterOrSetter(
                        propertySource,
                        baseSession,
                        FirDeclarationOrigin.Source,
                        propertyTypeRef,
                        accessorVisibility,
                        isGetter
                    )
                    .also {
                        if (this != null) {
                            it.extractAnnotationsFrom(this)
                        }
                        it.status = status
                    }
            }
            val source = this.toFirSourceElement()
            val accessorTarget = FirFunctionTarget(labelName = null, isLambda = false)
            return buildPropertyAccessor {
                this.source = source
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = if (isGetter) {
                    returnTypeReference?.convertSafe() ?: propertyTypeRef
                } else {
                    returnTypeReference.toFirOrUnitType()
                }
                this.isGetter = isGetter
                this.status = status
                extractAnnotationsTo(this)
                this@RawFirBuilder.context.firFunctionTargets += accessorTarget
                extractValueParametersTo(this, propertyTypeRef)
                if (!isGetter && valueParameters.isEmpty()) {
                    valueParameters += buildDefaultSetterValueParameter {
                        this.source = source.fakeElement(FirFakeSourceElementKind.DefaultAccessor)
                        session = baseSession
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = propertyTypeRef
                        symbol = FirVariableSymbol(NAME_FOR_DEFAULT_VALUE_PARAMETER)
                    }
                }
                symbol = FirPropertyAccessorSymbol()
                val (body, contractDescription) = this@toFirPropertyAccessor.buildFirBody()
                this.body = body
                contractDescription?.let {
                    this.contractDescription = it
                }
            }.also {
                accessorTarget.bind(it)
                this@RawFirBuilder.context.firFunctionTargets.removeLast()
            }
        }

        private fun KtParameter.toFirValueParameter(defaultTypeRef: FirTypeRef? = null): FirValueParameter {
            val name = nameAsSafeName
            return buildValueParameter {
                source = toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultTypeRef != null -> defaultTypeRef
                    else -> null.toFirOrImplicitType()
                }
                this.name = name
                symbol = FirVariableSymbol(name)
                defaultValue = if (hasDefaultValue()) {
                    { this@toFirValueParameter.defaultValue }.toFirExpression("Should have default value")
                } else null
                isCrossinline = hasModifier(CROSSINLINE_KEYWORD)
                isNoinline = hasModifier(NOINLINE_KEYWORD)
                isVararg = isVarArg
                extractAnnotationsTo(this)
            }
        }

        private fun KtParameter.toFirProperty(firParameter: FirValueParameter, isExpect: Boolean): FirProperty {
            require(hasValOrVar())
            val type = typeReference.toFirOrErrorType()
            val status = FirDeclarationStatusImpl(visibility, modality).apply {
                this.isExpect = isExpect
                isActual = hasActualModifier()
                isOverride = hasModifier(OVERRIDE_KEYWORD)
                isConst = false
                isLateInit = false
            }
            val propertySource = toFirSourceElement(FirFakeSourceElementKind.PropertyFromParameter)
            val propertyName = nameAsSafeName
            return buildProperty {
                source = propertySource
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = type
                receiverTypeRef = null
                name = propertyName
                initializer = buildQualifiedAccessExpression {
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
                val defaultAccessorSource = propertySource.fakeElement(FirFakeSourceElementKind.DefaultAccessor)
                getter = FirDefaultPropertyGetter(defaultAccessorSource, baseSession, FirDeclarationOrigin.Source, type, visibility)
                setter = if (isMutable) FirDefaultPropertySetter(
                    defaultAccessorSource,
                    baseSession,
                    FirDeclarationOrigin.Source,
                    type,
                    visibility
                ) else null
                extractAnnotationsTo(this)
            }.apply {
                isFromVararg = firParameter.isVararg
            }
        }

        private fun FirDefaultPropertyAccessor.extractAnnotationsFrom(annotated: KtAnnotated) {
            annotated.extractAnnotationsTo(this.annotations)
        }

        private fun KtAnnotated.extractAnnotationsTo(container: MutableList<FirAnnotationCall>) {
            for (annotationEntry in annotationEntries) {
                container += annotationEntry.convert<FirAnnotationCall>()
            }
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAnnotationContainerBuilder) {
            extractAnnotationsTo(container.annotations)
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(container: FirTypeParameterRefsOwnerBuilder) {
            for (typeParameter in typeParameters) {
                container.typeParameters += typeParameter.convert<FirTypeParameter>()
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(container: FirTypeParametersOwnerBuilder) {
            for (typeParameter in typeParameters) {
                container.typeParameters += typeParameter.convert<FirTypeParameter>()
            }
        }

        private fun KtDeclarationWithBody.extractValueParametersTo(
            container: FirFunctionBuilder,
            defaultTypeRef: FirTypeRef? = null,
        ) {
            for (valueParameter in valueParameters) {
                container.valueParameters += valueParameter.toFirValueParameter(defaultTypeRef)
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirCallBuilder) {
            val argumentList = buildArgumentList {
                for (argument in valueArguments) {
                    val argumentExpression = argument.toFirExpression()
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
            containerSymbol: AbstractFirBasedSymbol<*>
        ): FirTypeRef {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            var delegatedSuperTypeRef: FirTypeRef? = null
            var delegateNumber = 0
            val initializeDelegateStatements = mutableListOf<FirStatement>()
            for (superTypeListEntry in superTypeListEntries) {
                when (superTypeListEntry) {
                    is KtSuperTypeEntry -> {
                        container.superTypeRefs += superTypeListEntry.typeReference.toFirOrErrorType()
                    }
                    is KtSuperTypeCallEntry -> {
                        delegatedSuperTypeRef = superTypeListEntry.calleeExpression.typeReference.toFirOrErrorType()
                        container.superTypeRefs += delegatedSuperTypeRef
                        superTypeCallEntry = superTypeListEntry
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        val type = superTypeListEntry.typeReference.toFirOrErrorType()
                        val delegateExpression = { superTypeListEntry.delegateExpression }.toFirExpression("Should have delegate")
                        container.superTypeRefs += type
                        val delegateName = Name.special("<\$\$delegate_$delegateNumber>")
                        val delegateSource = superTypeListEntry.delegateExpression?.toFirSourceElement()
                        val delegateField = buildField {
                            source = delegateSource
                            session = baseSession
                            origin = FirDeclarationOrigin.Synthetic
                            name = delegateName
                            returnTypeRef = type
                            symbol = FirFieldSymbol(CallableId(name))
                            isVar = false
                            status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
                        }
                        initializeDelegateStatements.add(
                            buildVariableAssignment {
                                source = delegateSource
                                calleeReference =
                                    buildResolvedNamedReference {
                                        name = delegateName
                                        resolvedSymbol = delegateField.symbol
                                    }
                                rValue = delegateExpression
                                dispatchReceiver = buildThisReceiverExpression {
                                    calleeReference = buildImplicitThisReference {
                                        boundSymbol = containerSymbol
                                    }
                                    delegatedSelfTypeRef?.let { typeRef = it }
                                }
                            }
                        )
                        container.declarations.add(delegateField)
                        delegateNumber ++
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
                    container.superTypeRefs += delegatedSuperTypeRef
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
            if (this is KtClass && this.isInterface()) return delegatedSuperTypeRef ?: implicitAnyType

            // TODO: in case we have no primary constructor,
            // it may be not possible to determine delegated super type right here
            delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef
            if (!this.hasPrimaryConstructor()) return delegatedSuperTypeRef

            val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                superTypeCallEntry,
                delegatedSuperTypeRef,
                delegatedSelfTypeRef ?: delegatedSuperTypeRef,
                owner = this,
                containerTypeParameters,
                body = if (initializeDelegateStatements.isNotEmpty()) buildBlock {
                    for (statement in initializeDelegateStatements) {
                        statements += statement
                    }
                } else null
            )

            container.declarations += firPrimaryConstructor
            return delegatedSuperTypeRef
        }

        private fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            ownerTypeParameters: List<FirTypeParameterRef>,
            body: FirBlock? = null
        ): FirConstructor {
            val constructorCallee = superTypeCallEntry?.calleeExpression?.toFirSourceElement()
            val constructorSource = this?.toFirSourceElement()
                ?: owner.toFirPsiSourceElement(FirFakeSourceElementKind.ImplicitConstructor)
            val firDelegatedCall = buildDelegatedConstructorCall {
                source = constructorCallee ?: constructorSource.fakeElement(FirFakeSourceElementKind.DelegatingConstructorCall)
                constructedTypeRef = delegatedSuperTypeRef
                isThis = false
                if (!stubMode) {
                    superTypeCallEntry?.extractArgumentsTo(this)
                }
            }

            // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
            fun defaultVisibility() = when {
                owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) || owner is KtEnumEntry -> Visibilities.PRIVATE
                owner.hasModifier(SEALED_KEYWORD) -> Visibilities.PRIVATE
                else -> Visibilities.UNKNOWN
            }

            val explicitVisibility = this?.visibility
            val status = FirDeclarationStatusImpl(explicitVisibility ?: defaultVisibility(), Modality.FINAL).apply {
                isExpect = this@toFirConstructor?.hasExpectModifier() == true || owner.hasExpectModifier()
                isActual = this@toFirConstructor?.hasActualModifier() == true
                isInner = owner.hasModifier(INNER_KEYWORD)
                isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.PRIVATE
                isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
            }
            return buildPrimaryConstructor {
                source = constructorSource
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                this.status = status
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = firDelegatedCall
                typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                this@toFirConstructor?.extractAnnotationsTo(this)
                this@toFirConstructor?.extractValueParametersTo(this)
                this.body = body
            }
        }

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            context.packageFqName = file.packageFqName
            return buildFile {
                source = file.toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                name = file.name
                packageFqName = context.packageFqName
                for (annotationEntry in file.annotationEntries) {
                    annotations += annotationEntry.convert<FirAnnotationCall>()
                }
                for (importDirective in file.importDirectives) {
                    imports += buildImport {
                        source = importDirective.toFirSourceElement()
                        importedFqName = importDirective.importedFqName
                        isAllUnder = importDirective.isAllUnder
                        aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                    }
                }
                for (declaration in file.declarations) {
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
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedEnumSelfTypeRef
                name = nameAsSafeName
                status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
                    isStatic = true
                    isExpect = containingClassOrObject?.hasExpectModifier() == true
                }
                symbol = FirVariableSymbol(callableIdForName(nameAsSafeName))
                if (ownerClassHasDefaultConstructor && ktEnumEntry.initializerList == null &&
                    ktEnumEntry.annotationEntries.isEmpty() && ktEnumEntry.body == null
                ) {
                    return@buildEnumEntry
                }
                extractAnnotationsTo(this)
                initializer = withChildClassName(nameAsSafeName) {
                    buildAnonymousObject {
                        source = toFirSourceElement(FirFakeSourceElementKind.EnumInitializer)
                        session = baseSession
                        origin = FirDeclarationOrigin.Source
                        classKind = ClassKind.ENUM_ENTRY
                        scopeProvider = this@RawFirBuilder.baseScopeProvider
                        symbol = FirAnonymousObjectSymbol()

                        extractAnnotationsTo(this)
                        val delegatedEntrySelfType = buildResolvedTypeRef {
                            type = ConeClassLikeTypeImpl(this@buildAnonymousObject.symbol.toLookupTag(), emptyArray(), isNullable = false)
                        }
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

        override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit): FirElement {
            return withChildClassName(
                classOrObject.nameAsSafeName,
                classOrObject.isLocal || classOrObject.getStrictParentOfType<KtEnumEntry>() != null
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
                    if (classOrObject.isLocal) Visibilities.LOCAL else classOrObject.visibility,
                    classOrObject.modality,
                ).apply {
                    isExpect = classOrObject.hasExpectModifier()
                    isActual = classOrObject.hasActualModifier()
                    isInner = classOrObject.hasModifier(INNER_KEYWORD)
                    isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
                    isData = classOrObject.hasModifier(DATA_KEYWORD)
                    isInline = classOrObject.hasModifier(INLINE_KEYWORD)
                    isFun = classOrObject.hasModifier(FUN_KEYWORD)
                }
                withCapturedTypeParameters {
                    if (!status.isInner) context.capturedTypeParameters = context.capturedTypeParameters.clear()

                    buildRegularClass {
                        source = classOrObject.toFirSourceElement()
                        session = baseSession
                        origin = FirDeclarationOrigin.Source
                        name = classOrObject.nameAsSafeName
                        this.status = status
                        this.classKind = classKind
                        scopeProvider = baseScopeProvider
                        symbol = FirRegularClassSymbol(context.currentClassId)

                        classOrObject.extractAnnotationsTo(this)
                        classOrObject.extractTypeParametersTo(this)
                        typeParameters += context.capturedTypeParameters.map { buildOuterClassTypeParameterRef { symbol = it } }

                        addCapturedTypeParameters(typeParameters.take(classOrObject.typeParameters.size))

                        val delegatedSelfType = classOrObject.toDelegatedSelfType(this)
                        val delegatedSuperType = classOrObject.extractSuperTypeListEntriesTo(
                            this,
                            delegatedSelfType,
                            null,
                            classKind,
                            typeParameters,
                            symbol
                        )

                        val primaryConstructor = classOrObject.primaryConstructor
                        val firPrimaryConstructor = declarations.firstOrNull {it is FirConstructor} as? FirConstructor
                        if (primaryConstructor != null && firPrimaryConstructor != null) {
                            primaryConstructor.valueParameters.zip(
                                firPrimaryConstructor.valueParameters
                            ).forEach { (ktParameter, firParameter) ->
                                if (ktParameter.hasValOrVar()) {
                                    addDeclaration(ktParameter.toFirProperty(firParameter, classOrObject.hasExpectModifier()))
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
                            val zippedParameters = classOrObject.primaryConstructorParameters.zip(
                                declarations.filterIsInstance<FirProperty>(),
                            )
                            DataClassMembersGenerator(
                                baseSession,
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
                                        (property.returnTypeRef.psi as KtTypeReference).toFirOrImplicitType()
                                    }
                                },
                            ).generate()
                        }

                        if (classOrObject.hasModifier(ENUM_KEYWORD)) {
                            generateValuesFunction(
                                baseSession, context.packageFqName, context.className, classOrObject.hasExpectModifier()
                            )
                            generateValueOfFunction(
                                baseSession, context.packageFqName, context.className, classOrObject.hasExpectModifier()
                            )
                        }
                    }
                }
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return withChildClassName(ANONYMOUS_OBJECT_NAME) {
                buildAnonymousObject {
                    source = expression.toFirSourceElement()
                    session = baseSession
                    origin = FirDeclarationOrigin.Source
                    classKind = ClassKind.OBJECT
                    scopeProvider = baseScopeProvider
                    symbol = FirAnonymousObjectSymbol()
                    typeParameters += context.capturedTypeParameters.map { buildOuterClassTypeParameterRef { symbol = it } }
                    val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                    objectDeclaration.extractAnnotationsTo(this)
                    val delegatedSuperType = objectDeclaration.extractSuperTypeListEntriesTo(
                        this,
                        delegatedSelfType,
                        null,
                        ClassKind.CLASS,
                        containerTypeParameters = emptyList(),
                        symbol
                    )
                    typeRef = delegatedSelfType


                    for (declaration in objectDeclaration.declarations) {
                        declarations += declaration.toFirDeclaration(
                            delegatedSuperType,
                            delegatedSelfType,
                            owner = objectDeclaration,
                            ownerClassBuilder = this,
                            ownerTypeParameters = emptyList()
                        )
                    }
                }
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit): FirElement {
            return withChildClassName(typeAlias.nameAsSafeName) {
                buildTypeAlias {
                    source = typeAlias.toFirSourceElement()
                    session = baseSession
                    origin = FirDeclarationOrigin.Source
                    name = typeAlias.nameAsSafeName
                    status = FirDeclarationStatusImpl(typeAlias.visibility, Modality.FINAL).apply {
                        isExpect = typeAlias.hasExpectModifier()
                        isActual = typeAlias.hasActualModifier()
                    }
                    symbol = FirTypeAliasSymbol(context.currentClassId)
                    expandedTypeRef = typeAlias.getTypeReference().toFirOrErrorType()
                    typeAlias.extractAnnotationsTo(this)
                    typeAlias.extractTypeParametersTo(this)
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
            val functionIsAnonymousFunction = function.name == null && !function.parent.let { it is KtFile || it is KtClassBody }
            val functionBuilder = if (functionIsAnonymousFunction) {
                FirAnonymousFunctionBuilder().apply {
                    receiverTypeRef = receiverType
                    symbol = FirAnonymousFunctionSymbol()
                    isLambda = false
                    labelName = function.getLabelName()
                }
            } else {
                FirSimpleFunctionBuilder().apply {
                    receiverTypeRef = receiverType
                    name = function.nameAsSafeName
                    labelName = runIf(!name.isSpecial) { name.identifier }
                    symbol = FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName, function.isLocal))
                    status = FirDeclarationStatusImpl(
                        if (function.isLocal) Visibilities.LOCAL else function.visibility,
                        function.modality,
                    ).apply {
                        isExpect = function.hasExpectModifier() || function.containingClassOrObject?.hasExpectModifier() == true
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
            return functionBuilder.apply {
                source = function.toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType

                context.firFunctionTargets += target
                function.extractAnnotationsTo(this)
                if (this is FirSimpleFunctionBuilder) {
                    function.extractTypeParametersTo(this)
                }
                for (valueParameter in function.valueParameters) {
                    valueParameters += valueParameter.convert<FirValueParameter>()
                }
                withCapturedTypeParameters {
                    if (this is FirSimpleFunctionBuilder) addCapturedTypeParameters(this.typeParameters)
                    val (body, contractDescription) = function.buildFirBody()
                    this.body = body
                    contractDescription?.let {
                        // TODO: add error reporting for contracts on lambdas
                        if (this is FirSimpleFunctionBuilder) {
                            this.contractDescription = it
                        }
                    }
                }
                context.firFunctionTargets.removeLast()
            }.build().also {
                target.bind(it)
            }
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit): FirElement {
            val literal = expression.functionLiteral
            val literalSource = literal.toFirSourceElement()
            val implicitTypeRefSource = literal.toFirSourceElement(FirFakeSourceElementKind.ImplicitTypeRef)
            val returnType = buildImplicitTypeRef {
                source = implicitTypeRefSource
            }
            val receiverType = buildImplicitTypeRef {
                source = implicitTypeRefSource
            }

            val target: FirFunctionTarget
            return buildAnonymousFunction {
                source = literalSource
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = returnType
                receiverTypeRef = receiverType
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true

                var destructuringBlock: FirExpression? = null
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val name = Name.special("<destruct>")
                        val multiParameter = buildValueParameter {
                            source = valueParameter.toFirSourceElement()
                            session = baseSession
                            origin = FirDeclarationOrigin.Source
                            returnTypeRef = buildImplicitTypeRef {
                                source = multiDeclaration.toFirSourceElement(FirFakeSourceElementKind.ImplicitTypeRef)
                            }
                            this.name = name
                            symbol = FirVariableSymbol(name)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                        destructuringBlock = generateDestructuringBlock(
                            baseSession,
                            multiDeclaration,
                            multiParameter,
                            tmpVariable = false,
                            extractAnnotationsTo = { extractAnnotationsTo(it) },
                        ) { toFirOrImplicitType() }
                        multiParameter
                    } else {
                        val typeRef = buildImplicitTypeRef {
                            source = implicitTypeRefSource
                        }
                        valueParameter.toFirValueParameter(typeRef)
                    }
                }
                val expressionSource = expression.toFirSourceElement()
                label = context.firLabels.pop() ?: context.calleeNamesForLambda.lastOrNull()?.let {
                    buildLabel {
                        source = expressionSource.fakeElement(FirFakeSourceElementKind.GeneratedLambdaLabel)
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
                                    source = expressionSource.fakeElement(FirFakeSourceElementKind.ImplicitReturn)
                                    this.target = target
                                    result = buildUnitExpression {
                                        source = expressionSource.fakeElement(FirFakeSourceElementKind.ImplicitUnit)
                                    }
                                }
                            )
                        }
                        if (destructuringBlock is FirBlock) {
                            for ((index, statement) in destructuringBlock.statements.withIndex()) {
                                statements.add(index, statement)
                            }
                        }
                    }.build()
                }
                context.firFunctionTargets.removeLast()
            }.also {
                target.bind(it)
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
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = delegatedSelfTypeRef
                val explicitVisibility = visibility
                status = FirDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
                    isExpect = hasExpectModifier() || owner.hasExpectModifier()
                    isActual = hasActualModifier()
                    isInner = owner.hasModifier(INNER_KEYWORD)
                    isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.PRIVATE
                    isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                }
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = getDelegationCall().convert(
                    delegatedSuperTypeRef,
                    delegatedSelfTypeRef,
                )
                this@RawFirBuilder.context.firFunctionTargets += target
                extractAnnotationsTo(this)
                typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
                extractValueParametersTo(this)
                val (body, _) = buildFirBody()
                this.body = body
                this@RawFirBuilder.context.firFunctionTargets.removeLast()
            }.also {
                target.bind(it)
            }
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis //|| (isImplicit && hasPrimaryConstructor)
            val source = if (isImplicit) {
                this.toFirSourceElement(FirFakeSourceElementKind.ImplicitConstructor)
            } else {
                this.toFirSourceElement()
            }
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef
            }
            return buildDelegatedConstructorCall {
                this.source = source
                constructedTypeRef = delegatedType
                this.isThis = isThis
                if (!stubMode) {
                    extractArgumentsTo(this)
                }
            }
        }

        private fun KtProperty.toFirProperty(ownerClassBuilder: FirClassBuilder?): FirProperty {
            val propertyType = typeReference.toFirOrImplicitType()
            val propertyName = nameAsSafeName
            val isVar = isVar
            val propertyInitializer = if (hasInitializer()) {
                { initializer }.toFirExpression("Should have initializer")
            } else null
            val delegateExpression by lazy { delegate?.expression }
            val propertySource = toFirSourceElement()

            return buildProperty {
                source = propertySource
                session = baseSession
                origin = FirDeclarationOrigin.Source
                returnTypeRef = propertyType
                name = propertyName
                this.isVar = isVar

                initializer = propertyInitializer

                if (this@toFirProperty.isLocal) {
                    isLocal = true
                    symbol = FirPropertySymbol(propertyName)
                    val delegateBuilder = delegateExpression?.let {
                        FirWrappedDelegateExpressionBuilder().apply {
                            source = it.toFirSourceElement(FirFakeSourceElementKind.WrappedDelegate)
                            expression = it.toFirExpression("Incorrect delegate expression")
                        }
                    }

                    status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL).apply {
                        isLateInit = hasModifier(LATEINIT_KEYWORD)
                    }

                    val receiver = delegateExpression?.toFirExpression("Incorrect delegate expression")
                    generateAccessorsByDelegate(delegateBuilder, null, baseSession, isExtension = false, stubMode, receiver)
                } else {
                    isLocal = false
                    receiverTypeRef = receiverTypeReference.convertSafe()
                    symbol = FirPropertySymbol(callableIdForName(propertyName))
                    extractTypeParametersTo(this)
                    withCapturedTypeParameters {
                        addCapturedTypeParameters(this.typeParameters)
                        val delegateBuilder = if (hasDelegate()) {
                            FirWrappedDelegateExpressionBuilder().apply {
                                source =
                                    if (stubMode) null else delegateExpression?.toFirSourceElement(FirFakeSourceElementKind.WrappedDelegate)
                                expression = { delegateExpression }.toFirExpression("Should have delegate")
                            }
                        } else null

                        getter = this@toFirProperty.getter.toFirPropertyAccessor(this@toFirProperty, propertyType, isGetter = true)
                        setter = if (isVar) {
                            this@toFirProperty.setter.toFirPropertyAccessor(this@toFirProperty, propertyType, isGetter = false)
                        } else null

                        // Upward propagation of `inline` and `external` modifiers (from accessors to property)
                        // Note that, depending on `var` or `val`, checking setter's modifiers should be careful: for `val`, setter doesn't
                        // exist (null); for `var`, the retrieval of the specific modifier is supposed to be `true`
                        status = FirDeclarationStatusImpl(visibility, modality).apply {
                            isExpect = hasExpectModifier() || containingClassOrObject?.hasExpectModifier() == true
                            isActual = hasActualModifier()
                            isOverride = hasModifier(OVERRIDE_KEYWORD)
                            isConst = hasModifier(CONST_KEYWORD)
                            isLateInit = hasModifier(LATEINIT_KEYWORD)
                            isInline = hasModifier(INLINE_KEYWORD) || (getter!!.isInline && setter?.isInline != false)
                            isExternal = hasModifier(EXTERNAL_KEYWORD) || (getter!!.isExternal && setter?.isExternal != false)
                        }

                        val receiver = delegateExpression?.toFirExpression("Should have delegate")
                        generateAccessorsByDelegate(
                            delegateBuilder,
                            ownerClassBuilder,
                            baseSession,
                            isExtension = receiverTypeReference != null,
                            stubMode,
                            receiver
                        )
                    }
                }
                extractAnnotationsTo(this)
            }
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return buildAnonymousInitializer {
                source = initializer.toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                body = if (stubMode) buildEmptyExpressionBlock() else initializer.body.toFirBlock()
            }
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            return property.toFirProperty(ownerClassBuilder = null)
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val source = typeReference.toFirSourceElement()
            val isNullable = typeElement is KtNullableType

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                if (this is KtNullableType) this.innerType.unwrapNullable() else this

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
                                    referenceExpression!!.getReferencedNameAsName(),
                                    FirTypeArgumentListImpl(source).apply {
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
                        isSuspend = typeReference.hasModifier(SUSPEND_KEYWORD)
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
                null -> FirErrorTypeRefBuilder().apply {
                    this.source = source
                    diagnostic = ConeSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax)
                }
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (annotationEntry in typeReference.annotationEntries) {
                firTypeBuilder.annotations += annotationEntry.convert<FirAnnotationCall>()
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
                    source = this@buildAnnotationCall.source
                    this.name = name
                }
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit): FirElement {
            val parameterName = parameter.nameAsSafeName
            return buildTypeParameter {
                source = parameter.toFirSourceElement()
                session = baseSession
                origin = FirDeclarationOrigin.Source
                name = parameterName
                symbol = FirTypeParameterSymbol()
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

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit): FirElement {
            val projectionKind = typeProjection.projectionKind
            val projectionSource = typeProjection.toFirSourceElement()
            if (projectionKind == KtProjectionKind.STAR) {
                return buildStarProjection {
                    source = projectionSource
                }
            }
            if (projectionKind == KtProjectionKind.NONE && !stubMode && typeProjection.text == "_") {
                return FirTypePlaceholderProjection
            }
            val typeReference = typeProjection.typeReference
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
            parameter.toFirValueParameter()

        override fun visitBlockExpression(expression: KtBlockExpression, data: Unit): FirElement {
            return configureBlockWithoutBuilding(expression).build()
        }

        private fun configureBlockWithoutBuilding(expression: KtBlockExpression): FirBlockBuilder {
            return FirBlockBuilder().apply {
                source = expression.toFirSourceElement()
                for (statement in expression.statements) {
                    val firStatement = statement.toFirStatement("Statement expected: ${statement.text}")
                    if (firStatement !is FirBlock || firStatement.annotations.isNotEmpty()) {
                        statements += firStatement
                    } else {
                        statements += firStatement.statements
                    }
                }
            }
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit): FirElement {
            return generateAccessExpression(expression.toFirSourceElement(), expression.getReferencedNameAsName())
        }

        override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): FirElement =
            generateConstantExpressionByLiteral(expression)

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Unit): FirElement {
            return expression.entries.toInterpolatingCall(expression) {
                (this as KtStringTemplateEntryWithExpression).expression.toFirExpression(it)
            }
        }

        override fun visitReturnExpression(expression: KtReturnExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement(FirFakeSourceElementKind.ImplicitUnit)
            val result = expression.returnedExpression?.toFirExpression("Incorrect return expression")
                ?: buildUnitExpression { this.source = source }
            return result.toReturn(source, expression.getTargetLabel()?.getReferencedName())
        }

        override fun visitTryExpression(expression: KtTryExpression, data: Unit): FirElement {
            return buildTryExpression {
                source = expression.toFirSourceElement()
                tryBlock = expression.tryBlock.toFirBlock()
                finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.toFirValueParameter() ?: continue
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
                val ktCondition = expression.condition
                branches += buildWhenBranch {
                    source = ktCondition?.toFirSourceElement(FirFakeSourceElementKind.WhenCondition)
                    condition = ktCondition.toFirExpression("If statement should have condition")
                    result = expression.then.toFirBlock()
                }
                if (expression.elseKeyword != null) {
                    branches += buildWhenBranch {
                        condition = buildElseIfTrueCondition()
                        result = expression.`else`.toFirBlock()
                    }
                }
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
                        session = baseSession
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = ktSubjectExpression.typeReference.toFirOrImplicitType()
                        receiverTypeRef = null
                        this.name = name
                        initializer = subjectExpression
                        delegate = null
                        isVar = false
                        symbol = FirPropertySymbol(name)
                        isLocal = true
                        status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
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
                            val ktCondition = entry.conditions.first() as? KtWhenConditionWithExpression
                            buildWhenBranch {
                                source = entrySource
                                condition = ktCondition?.expression.toFirExpression("No expression in condition with expression")
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

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Unit): FirElement {
            return FirDoWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                condition = expression.condition.toFirExpression("No condition in do-while loop")
            }.configure { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: Unit): FirElement {
            return FirWhileLoopBuilder().apply {
                source = expression.toFirSourceElement()
                condition = expression.condition.toFirExpression("No condition in while loop")
            }.configure { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: Unit?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val ktParameter = expression.loopParameter
            val fakeSource = expression.toFirPsiSourceElement(FirFakeSourceElementKind.DesugaredForLoop)
            return buildBlock {
                source = fakeSource
                val rangeSource = expression.loopRange?.toFirSourceElement(FirFakeSourceElementKind.DesugaredForLoop)
                val iteratorVal = generateTemporaryVariable(
                    baseSession, rangeSource, Name.special("<iterator>"),
                    buildFunctionCall {
                        source = fakeSource
                        calleeReference = buildSimpleNamedReference {
                            source = fakeSource
                            name = Name.identifier("iterator")
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
                            name = Name.identifier("hasNext")
                        }
                        explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                    }
                }.configure {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val blockBuilder = when (val body = expression.body) {
                        is KtBlockExpression -> configureBlockWithoutBuilding(body)
                        null -> FirBlockBuilder()
                        else -> FirBlockBuilder().apply {
                            source = body.toFirSourceElement(FirFakeSourceElementKind.DesugaredForLoop)
                            statements += body.toFirStatement()
                        }
                    }
                    if (ktParameter != null) {
                        val multiDeclaration = ktParameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            session = baseSession, source = expression.loopParameter?.toFirSourceElement(),
                            name = if (multiDeclaration != null) Name.special("<destruct>") else ktParameter.nameAsSafeName,
                            initializer = buildFunctionCall {
                                source = fakeSource
                                calleeReference = buildSimpleNamedReference {
                                    source = fakeSource
                                    name = Name.identifier("next")
                                }
                                explicitReceiver = generateResolvedAccessExpression(fakeSource, iteratorVal)
                            },
                            typeRef = ktParameter.typeReference.toFirOrImplicitType(),
                        )
                        if (multiDeclaration != null) {
                            val destructuringBlock = generateDestructuringBlock(
                                session = baseSession,
                                multiDeclaration = multiDeclaration,
                                container = firLoopParameter,
                                tmpVariable = true,
                                extractAnnotationsTo = { extractAnnotationsTo(it) },
                            ) { toFirOrImplicitType() }
                            if (destructuringBlock is FirBlock) {
                                for ((index, statement) in destructuringBlock.statements.withIndex()) {
                                    blockBuilder.statements.add(index, statement)
                                }
                            }
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
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                if (firOperation in FirOperation.ASSIGNMENTS) {
                    return expression.left.generateAssignment(source, expression.right, rightArgument, firOperation) {
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
                    buildFunctionCall {
                        source = expression.toFirSourceElement()
                        calleeReference = buildSimpleNamedReference {
                            source = expression.operationReference.toFirSourceElement()
                            name = conventionCallName
                        }
                        explicitReceiver = argument.toFirExpression("No operand")
                    }
                }
                else -> throw IllegalStateException("Unexpected expression: ${expression.text}")
            }
        }

        private fun splitToCalleeAndReceiver(
            calleeExpression: KtExpression?,
            defaultSource: FirPsiSourceElement<*>,
        ): Pair<FirNamedReference, FirExpression?> {
            return when (calleeExpression) {
                is KtSimpleNameExpression -> buildSimpleNamedReference {
                    source = calleeExpression.toFirSourceElement()
                    name = calleeExpression.getReferencedNameAsName()
                } to null

                is KtParenthesizedExpression -> splitToCalleeAndReceiver(calleeExpression.expression, defaultSource)

                null -> {
                    buildErrorNamedReference { diagnostic = ConeSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax) } to null
                }

                is KtSuperExpression -> {
                    buildErrorNamedReference {
                        source = calleeExpression.toFirSourceElement()
                        diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed)
                    } to null
                }

                else -> {
                    buildSimpleNamedReference {
                        source = defaultSource.fakeElement(FirFakeSourceElementKind.ImplicitInvokeCall)
                        name = OperatorNameConventions.INVOKE
                    } to calleeExpression.toFirExpression("Incorrect invoke receiver")
                }
            }
        }

        override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement()
            val (calleeReference, explicitReceiver) = splitToCalleeAndReceiver(expression.calleeExpression, source)

            val result: FirQualifiedAccessBuilder = if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
                FirQualifiedAccessExpressionBuilder().apply {
                    this.source = source
                    this.calleeReference = calleeReference
                }
            } else {
                FirFunctionCallBuilder().apply {
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
            return buildFunctionCall {
                val source: FirPsiSourceElement<*>
                val getArgument = context.arraySetArgument.remove(expression)
                if (getArgument != null) {
                    calleeReference = buildSimpleNamedReference {
                        source = expression.parent.toFirSourceElement()
                        this.source = source.fakeElement(FirFakeSourceElementKind.ArrayAccessNameReference)
                        name = OperatorNameConventions.SET
                    }
                } else {
                    source = expression.toFirSourceElement()
                    calleeReference = buildSimpleNamedReference {
                        this.source = source.fakeElement(FirFakeSourceElementKind.ArrayAccessNameReference)
                        name = OperatorNameConventions.GET
                    }
                }
                this.source = source
                explicitReceiver = arrayExpression.toFirExpression("No array expression")
                argumentList = buildArgumentList {
                    for (indexExpression in expression.indexExpressions) {
                        arguments += indexExpression.toFirExpression("Incorrect index expression")
                    }
                    if (getArgument != null) {
                        arguments += getArgument
                    }
                }
            }
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Unit): FirElement {
            val selector = expression.selectorExpression
                ?: return buildErrorExpression(
                    expression.toFirSourceElement(), ConeSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax),
                )
            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirModifiableQualifiedAccess) {
                val receiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")

                if (expression is KtSafeQualifiedExpression) {
                    return firSelector.wrapWithSafeCall(receiver)
                }

                firSelector.explicitReceiver = receiver
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            return buildThisReceiverExpression {
                val sourceElement = expression.toFirSourceElement()
                source = sourceElement
                calleeReference = buildExplicitThisReference {
                    source = sourceElement.fakeElement(FirFakeSourceElementKind.ExplicitThisOrSuperReference)
                    labelName = expression.getLabelName()
                }
            }
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: Unit): FirElement {
            val superType = expression.superTypeQualifier
            val source = expression.toFirSourceElement()
            return buildQualifiedAccessExpression {
                this.source = source
                calleeReference = buildExplicitSuperReference {
                    source.fakeElement(FirFakeSourceElementKind.ExplicitThisOrSuperReference)
                    labelName = expression.getLabelName()
                    superTypeRef = superType.toFirOrImplicitType()
                }
            }
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): FirElement {
            return expression.expression?.accept(this, data)
                ?: buildErrorExpression(expression.toFirSourceElement(), ConeSimpleDiagnostic("Empty parentheses", DiagnosticKind.Syntax))
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): FirElement {
            val sourceElement = expression.toFirSourceElement()
            val label = expression.getTargetLabel()
            val size = context.firLabels.size
            if (label != null) {
                context.firLabels += buildLabel {
                    source = label.toFirPsiSourceElement()
                    name = label.getReferencedName()
                }
            }
            val result = expression.baseExpression?.accept(this, data)
                ?: buildErrorExpression(sourceElement, ConeSimpleDiagnostic("Empty label", DiagnosticKind.Syntax))
            if (size != context.firLabels.size) {
                context.firLabels.removeLast()
                println("Unused label: ${expression.text}")
            }
            return result
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Unit): FirElement {
            val rawResult = expression.baseExpression?.accept(this, data)
//            return rawResult ?: buildErrorExpression(
//                    expression.toFirSourceElement(),
//                    FirSimpleDiagnostic("Strange annotated expression: ${rawResult?.render()}", DiagnosticKind.Syntax),
//                )
            // TODO !!!!!!!!
            val result = rawResult as? FirAnnotationContainer
                ?: return buildErrorExpression(
                    expression.toFirSourceElement(),
                    ConeSimpleDiagnostic("Strange annotated expression: ${rawResult?.render()}", DiagnosticKind.Syntax),
                )
            expression.extractAnnotationsTo(result.annotations as MutableList<FirAnnotationCall>)
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
                baseSession, multiDeclaration.toFirSourceElement(), "destruct",
                multiDeclaration.initializer.toFirExpression("Destructuring declaration without initializer"),
            )
            return generateDestructuringBlock(
                baseSession,
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

    private val extensionFunctionAnnotation = buildAnnotationCall {
        annotationTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(ClassId.fromString(EXTENSION_FUNCTION_ANNOTATION)),
                emptyArray(),
                isNullable = false
            )
        }
        // TODO: actually we know where to resolve, but we don't have any symbol providers at this point
        calleeReference = buildSimpleNamedReference {
            name = Name.identifier("ExtensionFunctionType")
        }
    }
}
