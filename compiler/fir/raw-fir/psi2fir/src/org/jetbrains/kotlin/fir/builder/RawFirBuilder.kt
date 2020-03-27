/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class RawFirBuilder(
    session: FirSession, val baseScopeProvider: FirScopeProvider, val stubMode: Boolean
) : BaseFirBuilder<PsiElement>(session) {

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

    override fun PsiElement.toFirSourceElement(): FirPsiSourceElement<*> {
        return this.toFirPsiSourceElement()
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
                source = this@toFirOrImplicitType?.toFirSourceElement()
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
            delegatedSuperType: FirTypeRef, delegatedSelfType: FirResolvedTypeRef, owner: KtClassOrObject, hasPrimaryConstructor: Boolean,
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> {
                    toFirConstructor(
                        delegatedSuperType,
                        delegatedSelfType,
                        owner,
                        hasPrimaryConstructor,
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

        private fun KtDeclarationWithBody.buildFirBody(): FirBlock? =
            when {
                !hasBody() ->
                    null
                hasBlockBody() -> if (!stubMode) {
                    bodyBlockExpression?.accept(this@Visitor, Unit) as? FirBlock
                } else {
                    FirSingleExpressionBlock(buildExpressionStub { source = this@buildFirBody.toFirSourceElement() }.toReturn())
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    // basePsi is null, because 'return' is synthetic & should not be bound to some PSI
                    FirSingleExpressionBlock(result.toReturn(baseSource = null))
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
                    source = expression?.toFirSourceElement()
                    this.expression = firExpression
                    this.isSpread = isSpread
                    this.name = name
                }
                isSpread -> buildSpreadArgumentExpression {
                    source = expression?.toFirSourceElement()
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
            if (this == null || !hasBody()) {
                val propertySource = property.toFirSourceElement()
                val accessorVisibility = this?.visibility ?: property.visibility
                return FirDefaultPropertyAccessor
                    .createGetterOrSetter(propertySource, baseSession, propertyTypeRef, accessorVisibility, isGetter)
                    .also {
                        if (this != null) {
                            it.extractAnnotationsFrom(this)
                        }
                    }
            }
            val source = this.toFirSourceElement()
            val accessorTarget = FirFunctionTarget(labelName = null, isLambda = false)
            return buildPropertyAccessor {
                this.source = source
                session = baseSession
                returnTypeRef = if (isGetter) {
                    returnTypeReference?.convertSafe() ?: propertyTypeRef
                } else {
                    returnTypeReference.toFirOrUnitType()
                }
                this.isGetter = isGetter
                status = FirDeclarationStatusImpl(visibility, Modality.FINAL)
                extractAnnotationsTo(this)
                this@RawFirBuilder.context.firFunctionTargets += accessorTarget
                extractValueParametersTo(this, propertyTypeRef)
                if (!isGetter && valueParameters.isEmpty()) {
                    valueParameters += buildDefaultSetterValueParameter {
                        this.source = source
                        session = baseSession
                        returnTypeRef = propertyTypeRef
                        symbol = FirVariableSymbol(NAME_FOR_DEFAULT_VALUE_PARAMETER)
                    }
                }
                symbol = FirPropertyAccessorSymbol()
                body = this@toFirPropertyAccessor.buildFirBody()
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

        private fun KtParameter.toFirProperty(firParameter: FirValueParameter): FirProperty {
            require(hasValOrVar())
            var type = typeReference.toFirOrErrorType()
            if (firParameter.isVararg) {
                type = type.convertToArrayType()
            }
            val status = FirDeclarationStatusImpl(visibility, modality).apply {
                isExpect = hasExpectModifier()
                isActual = hasActualModifier()
                isOverride = hasModifier(OVERRIDE_KEYWORD)
                isConst = false
                isLateInit = false
            }
            val parameterSource = this.toFirSourceElement()
            val propertySource = this@toFirProperty.toFirSourceElement()
            val propertyName = nameAsSafeName
            return buildProperty {
                source = parameterSource
                session = baseSession
                returnTypeRef = type
                receiverTypeRef = null
                name = propertyName
                initializer = buildQualifiedAccessExpression {
                    source = parameterSource
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
                getter = FirDefaultPropertyGetter(propertySource, baseSession, type, visibility)
                setter = if (isMutable) FirDefaultPropertySetter(propertySource, baseSession, type, visibility) else null
                extractAnnotationsTo(this)
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
        ): FirTypeRef {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            var delegatedSuperTypeRef: FirTypeRef? = null
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
                        container.superTypeRefs += buildDelegatedTypeRef {
                            delegate = { superTypeListEntry.delegateExpression }.toFirExpression("Should have delegate")
                            typeRef = type
                        }
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
                            delegatedSelfTypeRef?.coneTypeUnsafe<ConeKotlinType>()?.let { arrayOf(it) } ?: emptyArray(),
                            isNullable = false,
                        )
                    }
                    container.superTypeRefs += delegatedSuperTypeRef
                }
                this is KtClass && classKind == ClassKind.ANNOTATION_CLASS -> {
                    container.superTypeRefs += implicitAnnotationType
                }
            }

            val defaultDelegatedSuperTypeRef =
                when {
                    classKind == ClassKind.ENUM_ENTRY && this is KtClass -> delegatedEnumSuperTypeRef ?: implicitAnyType
                    else -> implicitAnyType
                }


            if (container.superTypeRefs.isEmpty()) {
                container.superTypeRefs += defaultDelegatedSuperTypeRef
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
            )
            container.declarations += firPrimaryConstructor
            return delegatedSuperTypeRef
        }

        private fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
        ): FirConstructor {
            val constructorCallee = superTypeCallEntry?.calleeExpression?.toFirSourceElement()
            val constructorSource = (this ?: owner).toFirSourceElement()
            val firDelegatedCall = buildDelegatedConstructorCall {
                source = constructorCallee ?: constructorSource
                constructedTypeRef = delegatedSuperTypeRef
                isThis = false
                if (!stubMode) {
                    superTypeCallEntry?.extractArgumentsTo(this)
                }
            }

            fun defaultVisibility() = when {
                // TODO: object / enum is HIDDEN (?)
                owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) -> Visibilities.PRIVATE
                owner.hasModifier(SEALED_KEYWORD) -> Visibilities.PRIVATE
                else -> Visibilities.UNKNOWN
            }

            val explicitVisibility = this?.visibility
            val status = FirDeclarationStatusImpl(explicitVisibility ?: defaultVisibility(), Modality.FINAL).apply {
                isExpect = this@toFirConstructor?.hasExpectModifier() ?: false
                isActual = this@toFirConstructor?.hasActualModifier() ?: false
                isInner = owner.hasModifier(INNER_KEYWORD)
                isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.PRIVATE
                isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
            }
            return buildPrimaryConstructor {
                source = constructorSource
                session = baseSession
                returnTypeRef = delegatedSelfTypeRef
                this.status = status
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = firDelegatedCall
                typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
                this@toFirConstructor?.extractAnnotationsTo(this)
                this@toFirConstructor?.extractValueParametersTo(this)
            }
        }

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            context.packageFqName = file.packageFqName
            return buildFile {
                source = file.toFirSourceElement()
                session = baseSession
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
                returnTypeRef = delegatedEnumSelfTypeRef
                name = nameAsSafeName
                status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
                    isStatic = true
                }
                symbol = FirVariableSymbol(callableIdForName(nameAsSafeName))
                // NB: not sure should annotations be on enum entry itself, or on its corresponding object
                if (ownerClassHasDefaultConstructor && ktEnumEntry.initializerList == null &&
                    ktEnumEntry.annotationEntries.isEmpty() && ktEnumEntry.body == null
                ) {
                    return@buildEnumEntry
                }
                initializer = withChildClassName(nameAsSafeName) {
                    buildAnonymousObject {
                        source = toFirSourceElement()
                        session = baseSession
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
                        )
                        for (declaration in ktEnumEntry.declarations) {
                            declarations += declaration.toFirDeclaration(
                                correctedEnumSelfTypeRef,
                                delegatedSelfType = delegatedEntrySelfType,
                                ktEnumEntry,
                                hasPrimaryConstructor = true,
                            )
                        }
                    }
                }
            }
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit): FirElement {
            return withChildClassName(classOrObject.nameAsSafeName) {

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
                    isData = (classOrObject as? KtClass)?.isData() == true
                    isInline = classOrObject.hasModifier(INLINE_KEYWORD)
                }
                val classBuilder = if (status.modality == Modality.SEALED) FirSealedClassBuilder() else FirClassImplBuilder()
                classBuilder.apply {
                    source = classOrObject.toFirSourceElement()
                    session = baseSession
                    name = classOrObject.nameAsSafeName
                    this.status = status
                    this.classKind = classKind
                    scopeProvider = baseScopeProvider
                    symbol = FirRegularClassSymbol(context.currentClassId)

                    classOrObject.extractAnnotationsTo(this)
                    classOrObject.extractTypeParametersTo(this)

                    val delegatedSelfType = classOrObject.toDelegatedSelfType(this)
                    val delegatedSuperType = classOrObject.extractSuperTypeListEntriesTo(this, delegatedSelfType, null, classKind)

                    val primaryConstructor = classOrObject.primaryConstructor
                    val firPrimaryConstructor = declarations.firstOrNull() as? FirConstructor
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
                                delegatedSuperType, delegatedSelfType, classOrObject, hasPrimaryConstructor = primaryConstructor != null,
                            ),
                        )
                    }

                    if (classOrObject.hasModifier(DATA_KEYWORD) && firPrimaryConstructor != null) {
                        val zippedParameters = classOrObject.primaryConstructorParameters.zip(
                            declarations.filterIsInstance<FirProperty>(),
                        )
                        zippedParameters.generateComponentFunctions(
                            baseSession, this, context.packageFqName, context.className, firPrimaryConstructor,
                        )
                        zippedParameters.generateCopyFunction(
                            baseSession, classOrObject, this, context.packageFqName, context.className, firPrimaryConstructor,
                        )
                        // TODO: equals, hashCode, toString
                    }

                    if (classOrObject.hasModifier(ENUM_KEYWORD)) {
                        generateValuesFunction(baseSession, context.packageFqName, context.className)
                        generateValueOfFunction(baseSession, context.packageFqName, context.className)
                    }
                }.build()
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return withChildClassName(ANONYMOUS_OBJECT_NAME) {
                buildAnonymousObject {
                    source = expression.toFirSourceElement()
                    session = baseSession
                    classKind = ClassKind.OBJECT
                    scopeProvider = baseScopeProvider
                    symbol = FirAnonymousObjectSymbol()
                    val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                    objectDeclaration.extractAnnotationsTo(this)
                    val delegatedSuperType = objectDeclaration.extractSuperTypeListEntriesTo(this, delegatedSelfType, null, ClassKind.CLASS)
                    typeRef = delegatedSelfType

                    for (declaration in objectDeclaration.declarations) {
                        declarations += declaration.toFirDeclaration(
                            delegatedSuperType,
                            delegatedSelfType,
                            owner = objectDeclaration,
                            hasPrimaryConstructor = false,
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

            val functionBuilder = if (function.name == null) {
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
                    labelName = name.identifier
                    symbol = FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName, function.isLocal))
                    status = FirDeclarationStatusImpl(
                        if (function.isLocal) Visibilities.LOCAL else function.visibility,
                        function.modality,
                    ).apply {
                        isExpect = function.hasExpectModifier()
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
                returnTypeRef = returnType

                context.firFunctionTargets += target
                function.extractAnnotationsTo(this)
                if (this is FirSimpleFunctionBuilder) {
                    function.extractTypeParametersTo(this)
                }
                for (valueParameter in function.valueParameters) {
                    valueParameters += valueParameter.convert<FirValueParameter>()
                }
                body = function.buildFirBody()
                context.firFunctionTargets.removeLast()
            }.build().also {
                target.bind(it)
            }
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit): FirElement {
            val literal = expression.functionLiteral
            val literalSource = literal.toFirSourceElement()
            val returnType = buildImplicitTypeRef {
                source = literalSource
            }
            val receiverType = buildImplicitTypeRef {
                source = literalSource
            }

            val target: FirFunctionTarget
            return buildAnonymousFunction {
                source = literalSource
                session = baseSession
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
                            returnTypeRef = buildImplicitTypeRef {
                                source = multiDeclaration.toFirSourceElement()
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
                            source = this@buildAnonymousFunction.source
                        }
                        valueParameter.toFirValueParameter(typeRef)
                    }
                }
                val expressionSource = expression.toFirSourceElement()
                label = context.firLabels.pop() ?: context.firFunctionCalls.lastOrNull()?.calleeReference?.name?.let {
                    buildLabel {
                        source = expressionSource
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
                            statements.add(buildUnitExpression { source = expressionSource })
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
            hasPrimaryConstructor: Boolean,
        ): FirConstructor {
            val target = FirFunctionTarget(labelName = null, isLambda = false)
            return buildConstructor {
                source = this@toFirConstructor.toFirSourceElement()
                session = baseSession
                returnTypeRef = delegatedSelfTypeRef
                val explicitVisibility = visibility
                status = FirDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
                    isExpect = hasExpectModifier()
                    isActual = hasActualModifier()
                    isInner = owner.hasModifier(INNER_KEYWORD)
                    isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.PRIVATE
                    isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
                }
                symbol = FirConstructorSymbol(callableIdForClassConstructor())
                delegatedConstructor = getDelegationCall().convert(
                    delegatedSuperTypeRef,
                    delegatedSelfTypeRef,
                    hasPrimaryConstructor
                )
                this@RawFirBuilder.context.firFunctionTargets += target
                extractAnnotationsTo(this)
                typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
                extractValueParametersTo(this)
                body = buildFirBody()
                this@RawFirBuilder.context.firFunctionTargets.removeLast()
            }.also {
                target.bind(it)
            }
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            hasPrimaryConstructor: Boolean,
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis || (isImplicit && hasPrimaryConstructor)
            val source = this.toFirSourceElement()
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

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return buildAnonymousInitializer {
                source = initializer.toFirSourceElement()
                session = baseSession
                body = if (stubMode) buildEmptyExpressionBlock() else initializer.body.toFirBlock()
            }
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            val propertyType = property.typeReference.toFirOrImplicitType()
            val propertyName = property.nameAsSafeName
            val isVar = property.isVar
            val propertyInitializer = if (property.hasInitializer()) {
                { property.initializer }.toFirExpression("Should have initializer")
            } else null
            val delegateExpression by lazy { property.delegate?.expression }
            val propertySource = property.toFirSourceElement()

            return buildProperty {
                source = propertySource
                session = baseSession
                returnTypeRef = propertyType
                name = propertyName
                this.isVar = isVar
                initializer = propertyInitializer

                if (property.isLocal) {
                    isLocal = true
                    symbol = FirPropertySymbol(propertyName)
                    val delegateBuilder = delegateExpression?.let {
                        FirWrappedDelegateExpressionBuilder().apply {
                            source = it.toFirSourceElement()
                            expression = it.toFirExpression("Incorrect delegate expression")
                        }
                    }

                    status = FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)

                    val receiver = delegateExpression?.toFirExpression("Incorrect delegate expression")
                    generateAccessorsByDelegate(delegateBuilder, baseSession, member = false, extension = false, stubMode, receiver)
                } else {
                    isLocal = false
                    receiverTypeRef = property.receiverTypeReference.convertSafe()
                    symbol = FirPropertySymbol(callableIdForName(propertyName))
                    val delegateBuilder = if (property.hasDelegate()) {
                        FirWrappedDelegateExpressionBuilder().apply {
                            source = if (stubMode) null else delegateExpression?.toFirSourceElement()
                            expression = { delegateExpression }.toFirExpression("Should have delegate")
                        }
                    } else null
                    status = FirDeclarationStatusImpl(property.visibility, property.modality).apply {
                        isExpect = property.hasExpectModifier()
                        isActual = property.hasActualModifier()
                        isOverride = property.hasModifier(OVERRIDE_KEYWORD)
                        isConst = property.hasModifier(CONST_KEYWORD)
                        isLateInit = property.hasModifier(LATEINIT_KEYWORD)
                    }
                    property.extractTypeParametersTo(this)

                    getter = property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true)
                    setter = if (isVar) property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false) else null

                    val receiver = delegateExpression?.toFirExpression("Should have delegate")
                    generateAccessorsByDelegate(
                        delegateBuilder,
                        baseSession,
                        member = !property.isTopLevel,
                        extension = property.receiverTypeReference != null,
                        stubMode,
                        receiver
                    )
                }
                property.extractAnnotationsTo(this)
            }
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
                                val firQualifier = FirQualifierPartImpl(referenceExpression!!.getReferencedNameAsName())
                                for (typeArgument in ktQualifier!!.typeArguments) {
                                    firQualifier.typeArguments += typeArgument.convert<FirTypeProjection>()
                                }
                                qualifier.add(firQualifier)

                                ktQualifier = ktQualifier.qualifier
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
            }
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit): FirElement {
            val parameterName = parameter.nameAsSafeName
            return buildTypeParameter {
                source = parameter.toFirSourceElement()
                session = baseSession
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
            if (projectionKind == KtProjectionKind.NONE && typeProjection.text == "_") {
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
            val source = expression.toFirSourceElement()
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
                    source = ktCondition?.toFirSourceElement()
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
            val subject = FirWhenSubject()
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
                                    entrySource,
                                    subject,
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
                    subject.bind(it)
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
            val loopSource = expression.toFirSourceElement()
            return buildBlock {
                source = loopSource
                val rangeSource = expression.loopRange?.toFirSourceElement()
                val iteratorVal = generateTemporaryVariable(
                    baseSession, rangeSource, Name.special("<iterator>"),
                    buildFunctionCall {
                        source = loopSource
                        calleeReference = buildSimpleNamedReference {
                            source = loopSource
                            name = Name.identifier("iterator")
                        }
                        explicitReceiver = rangeExpression
                    },
                )
                statements += iteratorVal
                statements += FirWhileLoopBuilder().apply {
                    source = loopSource
                    condition = buildFunctionCall {
                        source = loopSource
                        calleeReference = buildSimpleNamedReference {
                            source = loopSource
                            name = Name.identifier("hasNext")
                        }
                        explicitReceiver = generateResolvedAccessExpression(loopSource, iteratorVal)
                    }
                }.configure {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val blockBuilder = when (val body = expression.body) {
                        is KtBlockExpression -> configureBlockWithoutBuilding(body)
                        null -> FirBlockBuilder()
                        else -> FirBlockBuilder().apply {
                            source = body.toFirSourceElement()
                            statements += body.toFirStatement()
                        }
                    }
                    if (ktParameter != null) {
                        val multiDeclaration = ktParameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            session = baseSession, source = expression.loopParameter?.toFirSourceElement(),
                            name = if (multiDeclaration != null) Name.special("<destruct>") else ktParameter.nameAsSafeName,
                            initializer = buildFunctionCall {
                                source = loopSource
                                calleeReference = buildSimpleNamedReference {
                                    source = loopSource
                                    name = Name.identifier("next")
                                }
                                explicitReceiver = generateResolvedAccessExpression(loopSource, iteratorVal)
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
            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")
            val source = expression.toFirSourceElement()
            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(baseSession, rightArgument, "elvis", source)
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
                    buildOperatorCall {
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
                else -> {
                    buildOperatorCall {
                        source = expression.toFirSourceElement()
                        operation = operationToken.toFirOperation()
                        argumentList = buildUnaryArgumentList(argument.toFirExpression("No operand"))
                    }
                }
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
                        source = defaultSource
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
                    context.firFunctionCalls += this
                    expression.extractArgumentsTo(this)
                    context.firFunctionCalls.removeLast()
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
                        this.source = source
                        name = OperatorNameConventions.SET
                    }
                } else {
                    source = expression.toFirSourceElement()
                    calleeReference = buildSimpleNamedReference {
                        this.source = source
                        name = OperatorNameConventions.GET
                    }
                }
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
                firSelector.safe = expression is KtSafeQualifiedExpression
                firSelector.explicitReceiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            return buildThisReceiverExpression {
                val sourceElement = expression.toFirSourceElement()
                source = sourceElement
                calleeReference = buildExplicitThisReference {
                    source = sourceElement
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
                    this.source
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
            val labelName = expression.getLabelName()
            val size = context.firLabels.size
            if (labelName != null) {
                context.firLabels += buildLabel {
                    source = sourceElement
                    name = labelName
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
                safe = expression.hasQuestionMarks
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
    }
}
