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
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.impl.FirLabelImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.impl.*
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

class RawFirBuilder(session: FirSession, val scopeProvider: FirScopeProvider, val stubMode: Boolean) : BaseFirBuilder<PsiElement>(session) {

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
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
        return (this as KtExpressionWithLabel).getLabelName()
    }

    override fun PsiElement.getExpressionInParentheses(): PsiElement? {
        return (this as KtParenthesizedExpression).expression
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
            convertSafe() ?: FirImplicitTypeRefImpl(this?.toFirSourceElement())

        private fun KtTypeReference?.toFirOrUnitType(): FirTypeRef =
            convertSafe() ?: implicitUnitType

        private fun KtTypeReference?.toFirOrErrorType(): FirTypeRef =
            convertSafe() ?: FirErrorTypeRefImpl(
                this?.toFirSourceElement(),
                FirSimpleDiagnostic(if (this == null) "Incomplete code" else "Conversion failed", DiagnosticKind.Syntax)
            )

        // Here we accept lambda as receiver to prevent expression calculation in stub mode
        private fun (() -> KtExpression?).toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(null)
            else with(this()) {
                convertSafe<FirExpression>() ?: FirErrorExpressionImpl(
                    this?.toFirSourceElement(), FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax)
                )
            }

        private fun KtExpression?.toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(null)
            else convertSafe<FirExpression>() ?: FirErrorExpressionImpl(
                this?.toFirSourceElement(), FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax)
            )

        private fun KtExpression.toFirStatement(errorReason: String): FirStatement =
            convertSafe() ?: FirErrorExpressionImpl(this.toFirSourceElement(), FirSimpleDiagnostic(errorReason, DiagnosticKind.Syntax))

        private fun KtExpression.toFirStatement(): FirStatement =
            convert()

        private fun KtDeclaration.toFirDeclaration(
            delegatedSuperType: FirTypeRef?, delegatedSelfType: FirResolvedTypeRef?, owner: KtClassOrObject, hasPrimaryConstructor: Boolean
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> toFirConstructor(
                    delegatedSuperType,
                    delegatedSelfType ?: FirErrorTypeRefImpl(
                        this.toFirSourceElement(), FirSimpleDiagnostic("Constructor in object", DiagnosticKind.ConstructorInObject)
                    ),
                    owner,
                    hasPrimaryConstructor
                )
                is KtEnumEntry -> toFirEnumEntry(delegatedSelfType!!)
                else -> convert()
            }
        }

        private fun KtExpression?.toFirBlock(): FirBlock =
            when (this) {
                is KtBlockExpression ->
                    accept(this@Visitor, Unit) as FirBlock
                null ->
                    FirEmptyExpressionBlock()
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
                    FirSingleExpressionBlock(FirExpressionStub(this.toFirSourceElement()).toReturn())
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    // basePsi is null, because 'return' is synthetic & should not be bound to some PSI
                    FirSingleExpressionBlock(result.toReturn(baseSource = null))
                }
            }

        private fun ValueArgument?.toFirExpression(): FirExpression {
            if (this == null) {
                return FirErrorExpressionImpl(
                    (this as? KtElement)?.toFirSourceElement(),
                    FirSimpleDiagnostic("No argument given", DiagnosticKind.Syntax)
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
                name != null -> FirNamedArgumentExpressionImpl(expression?.toFirSourceElement(), firExpression, isSpread, name)
                isSpread -> FirSpreadArgumentExpressionImpl(expression?.toFirSourceElement(), firExpression)
                else -> firExpression
            }
        }

        private fun KtPropertyAccessor?.toFirPropertyAccessor(
            property: KtProperty,
            propertyTypeRef: FirTypeRef,
            isGetter: Boolean
        ): FirPropertyAccessor {
            if (this == null) {
                val propertySource = property.toFirSourceElement()
                return if (isGetter) {
                    FirDefaultPropertyGetter(propertySource, session, propertyTypeRef, property.visibility)
                } else {
                    FirDefaultPropertySetter(propertySource, session, propertyTypeRef, property.visibility)
                }
            }
            val source = this.toFirSourceElement()
            val firAccessor = FirPropertyAccessorImpl(
                source,
                session,
                if (isGetter) {
                    returnTypeReference?.convertSafe() ?: propertyTypeRef
                } else {
                    returnTypeReference.toFirOrUnitType()
                },
                FirPropertyAccessorSymbol(),
                isGetter,
                FirDeclarationStatusImpl(visibility, Modality.FINAL)
            )
            this@RawFirBuilder.context.firFunctions += firAccessor
            extractAnnotationsTo(firAccessor)
            extractValueParametersTo(firAccessor, propertyTypeRef)
            if (!isGetter && firAccessor.valueParameters.isEmpty()) {
                firAccessor.valueParameters += FirDefaultSetterValueParameter(
                    source,
                    session,
                    propertyTypeRef,
                    FirVariableSymbol(NAME_FOR_DEFAULT_VALUE_PARAMETER)
                )
            }
            firAccessor.body = this.buildFirBody()
            this@RawFirBuilder.context.firFunctions.removeLast()
            return firAccessor
        }

        private fun KtParameter.toFirValueParameter(defaultTypeRef: FirTypeRef? = null): FirValueParameter {
            val name = nameAsSafeName
            val firValueParameter = FirValueParameterImpl(
                this.toFirSourceElement(),
                session,
                when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultTypeRef != null -> defaultTypeRef
                    else -> null.toFirOrImplicitType()
                },
                name,
                FirVariableSymbol(name),
                if (hasDefaultValue()) {
                    { defaultValue }.toFirExpression("Should have default value")
                } else null,
                isCrossinline = hasModifier(CROSSINLINE_KEYWORD),
                isNoinline = hasModifier(NOINLINE_KEYWORD),
                isVararg = isVarArg
            )
            extractAnnotationsTo(firValueParameter)
            return firValueParameter
        }

        private fun KtParameter.toFirProperty(firParameter: FirValueParameter): FirProperty {
            require(hasValOrVar())
            val type = typeReference.toFirOrErrorType()
            val status = FirDeclarationStatusImpl(visibility, modality).apply {
                isExpect = hasExpectModifier()
                isActual = hasActualModifier()
                isOverride = hasModifier(OVERRIDE_KEYWORD)
                isConst = false
                isLateInit = false
            }
            val parameterSource = this.toFirSourceElement()
            val propertySource = this@toFirProperty.toFirSourceElement()
            val firProperty = FirPropertyImpl(
                parameterSource,
                session,
                type,
                null,
                nameAsSafeName,
                FirQualifiedAccessExpressionImpl(parameterSource).apply {
                    calleeReference = FirPropertyFromParameterResolvedNamedReference(
                        propertySource, nameAsSafeName, firParameter.symbol
                    )
                },
                null,
                isMutable,
                FirPropertySymbol(callableIdForName(nameAsSafeName)),
                false,
                status
            ).apply {
                getter = FirDefaultPropertyGetter(propertySource, this@RawFirBuilder.session, type, visibility)
                setter = if (isMutable) FirDefaultPropertySetter(propertySource, this@RawFirBuilder.session, type, visibility) else null
            }
            extractAnnotationsTo(firProperty)
            return firProperty
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAbstractAnnotatedElement) {
            for (annotationEntry in annotationEntries) {
                container.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(container: FirModifiableTypeParametersOwner) {
            for (typeParameter in typeParameters) {
                container.typeParameters += typeParameter.convert<FirTypeParameter>()
            }
        }

        private fun KtDeclarationWithBody.extractValueParametersTo(
            container: FirFunction<*>,
            defaultTypeRef: FirTypeRef? = null
        ) {
            for (valueParameter in valueParameters) {
                (container.valueParameters as MutableList<FirValueParameter>) += valueParameter.toFirValueParameter(defaultTypeRef)
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirCallWithArgumentList) {
            for (argument in this.valueArguments) {
                val argumentExpression = argument.toFirExpression()
                container.arguments += when (argument) {
                    is KtLambdaArgument -> FirLambdaArgumentExpressionImpl(argument.toFirSourceElement(), argumentExpression)
                    else -> argumentExpression
                }
            }
        }

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(
            container: FirModifiableClass<*>,
            delegatedSelfTypeRef: FirTypeRef?,
            delegatedEnumSuperTypeRef: FirTypeRef?,
            classKind: ClassKind
        ): FirTypeRef? {
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
                        container.superTypeRefs += FirDelegatedTypeRefImpl(
                            { superTypeListEntry.delegateExpression }.toFirExpression("Should have delegate"),
                            type
                        )
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
                    container.superTypeRefs += FirResolvedTypeRefImpl(
                        null,
                        ConeClassLikeTypeImpl(
                            implicitEnumType.type.lookupTag,
                            delegatedSelfTypeRef?.coneTypeUnsafe<ConeKotlinType>()?.let { arrayOf(it) } ?: emptyArray(),
                            isNullable = false
                        )
                    )
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
            if (this is KtClass && this.isInterface()) return delegatedSuperTypeRef

            // TODO: in case we have no primary constructor,
            // it may be not possible to determine delegated super type right here
            delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef
            if (!this.hasPrimaryConstructor()) return delegatedSuperTypeRef

            val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                superTypeCallEntry,
                delegatedSuperTypeRef,
                delegatedSelfTypeRef ?: delegatedSuperTypeRef,
                owner = this
            )
            container.declarations += firPrimaryConstructor
            return delegatedSuperTypeRef
        }

        private fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperTypeRef: FirTypeRef,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject
        ): FirConstructor {
            val constructorCallee = superTypeCallEntry?.calleeExpression?.toFirSourceElement()
            val constructorSource = (this ?: owner).toFirSourceElement()
            val firDelegatedCall = FirDelegatedConstructorCallImpl(
                constructorCallee ?: constructorSource,
                delegatedSuperTypeRef,
                isThis = false
            ).apply {
                if (!stubMode) {
                    superTypeCallEntry?.extractArgumentsTo(this)
                }
            }

            fun defaultVisibility() = when {
                // TODO: object / enum is HIDDEN (?)
                owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) -> Visibilities.PRIVATE
                owner.hasModifier(SEALED_KEYWORD) -> Visibilities.PUBLIC
                else -> Visibilities.UNKNOWN
            }

            val status = FirDeclarationStatusImpl(this?.visibility ?: defaultVisibility(), Modality.FINAL).apply {
                isExpect = this@toFirConstructor?.hasExpectModifier() ?: false
                isActual = this@toFirConstructor?.hasActualModifier() ?: false
                isInner = owner.hasModifier(INNER_KEYWORD)
            }
            val firConstructor = FirPrimaryConstructorImpl(
                constructorSource,
                session,
                delegatedSelfTypeRef,
                null,
                status,
                FirConstructorSymbol(callableIdForClassConstructor())
            ).apply {
                delegatedConstructor = firDelegatedCall
            }
            this?.extractAnnotationsTo(firConstructor)
            firConstructor.typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
            this?.extractValueParametersTo(firConstructor)
            return firConstructor
        }

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            context.packageFqName = file.packageFqName
            val firFile = FirFileImpl(file.toFirSourceElement(), session, file.name, context.packageFqName)
            for (annotationEntry in file.annotationEntries) {
                firFile.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            for (importDirective in file.importDirectives) {
                firFile.imports += FirImportImpl(
                    importDirective.toFirSourceElement(),
                    importDirective.importedFqName,
                    importDirective.isAllUnder,
                    importDirective.aliasName?.let { Name.identifier(it) }
                )
            }
            for (declaration in file.declarations) {
                firFile.declarations += declaration.convert<FirDeclaration>()
            }
            return firFile
        }

        private fun KtEnumEntry.toFirEnumEntry(delegatedEnumSelfTypeRef: FirResolvedTypeRef): FirDeclaration {
            return FirEnumEntryImpl(
                source = toFirSourceElement(),
                session,
                delegatedEnumSelfTypeRef,
                name = nameAsSafeName,
                initializer = withChildClassName(nameAsSafeName) {
                    val obj = FirAnonymousObjectImpl(
                        source = toFirSourceElement(),
                        session,
                        ClassKind.ENUM_ENTRY,
                        scopeProvider,
                        FirAnonymousObjectSymbol()
                    )

                    val delegatedEntrySelfType = FirResolvedTypeRefImpl(
                        source = null,
                        type = ConeClassLikeTypeImpl(obj.symbol.toLookupTag(), emptyArray(), isNullable = false)
                    )

                    extractAnnotationsTo(obj)
                    obj.superTypeRefs += delegatedEnumSelfTypeRef
                    val superTypeCallEntry = superTypeListEntries.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                    val correctedEnumSelfTypeRef = FirResolvedTypeRefImpl(
                        source = superTypeCallEntry?.calleeExpression?.typeReference?.toFirSourceElement(),
                        type = delegatedEnumSelfTypeRef.type
                    )
                    obj.declarations += primaryConstructor.toFirConstructor(
                        superTypeCallEntry,
                        correctedEnumSelfTypeRef,
                        delegatedEntrySelfType,
                        owner = this
                    )

                    for (declaration in declarations) {
                        obj.declarations += declaration.toFirDeclaration(
                            correctedEnumSelfTypeRef,
                            delegatedSelfType = delegatedEntrySelfType,
                            this,
                            hasPrimaryConstructor = true
                        )
                    }
                    obj
                },
                status = FirDeclarationStatusImpl(
                    Visibilities.PUBLIC, Modality.FINAL
                ).apply {
                    isStatic = true
                },
                symbol = FirVariableSymbol(callableIdForName(nameAsSafeName))
            )
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
                    classOrObject.modality
                ).apply {
                    isExpect = classOrObject.hasExpectModifier()
                    isActual = classOrObject.hasActualModifier()
                    isInner = classOrObject.hasModifier(INNER_KEYWORD)
                    isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true
                    isData = (classOrObject as? KtClass)?.isData() == true
                    isInline = classOrObject.hasModifier(INLINE_KEYWORD)
                }
                val firClass = if (status.modality == Modality.SEALED) {
                    FirSealedClassImpl(
                        classOrObject.toFirSourceElement(),
                        session,
                        classOrObject.nameAsSafeName,
                        status,
                        classKind,
                        scopeProvider,
                        FirRegularClassSymbol(context.currentClassId)
                    )
                } else {
                    FirClassImpl(
                        classOrObject.toFirSourceElement(),
                        session,
                        classOrObject.nameAsSafeName,
                        status,
                        classKind,
                        scopeProvider,
                        FirRegularClassSymbol(context.currentClassId)
                    )
                }
                classOrObject.extractAnnotationsTo(firClass)
                classOrObject.extractTypeParametersTo(firClass)
                val delegatedSelfType = classOrObject.toDelegatedSelfType(firClass)
                val delegatedSuperType = classOrObject.extractSuperTypeListEntriesTo(firClass, delegatedSelfType, null, classKind)
                val primaryConstructor = classOrObject.primaryConstructor
                val firPrimaryConstructor = firClass.declarations.firstOrNull() as? FirConstructor
                if (primaryConstructor != null && firPrimaryConstructor != null) {
                    primaryConstructor.valueParameters.zip(firPrimaryConstructor.valueParameters).forEach { (ktParameter, firParameter) ->
                        if (ktParameter.hasValOrVar()) {
                            firClass.addDeclaration(ktParameter.toFirProperty(firParameter))
                        }
                    }
                }

                for (declaration in classOrObject.declarations) {
                    firClass.addDeclaration(
                        declaration.toFirDeclaration(
                            delegatedSuperType, delegatedSelfType, classOrObject, hasPrimaryConstructor = primaryConstructor != null
                        )
                    )
                }

                if (classOrObject.hasModifier(DATA_KEYWORD) && firPrimaryConstructor != null) {
                    val zippedParameters = classOrObject.primaryConstructorParameters.zip(
                        firClass.declarations.filterIsInstance<FirProperty>()
                    )
                    zippedParameters.generateComponentFunctions(
                        session, firClass, context.packageFqName, context.className, firPrimaryConstructor
                    )
                    zippedParameters.generateCopyFunction(
                        session, classOrObject, firClass, context.packageFqName, context.className, firPrimaryConstructor
                    )
                    // TODO: equals, hashCode, toString
                }

                if (classOrObject.hasModifier(ENUM_KEYWORD)) {
                    firClass.generateValuesFunction(session, context.packageFqName, context.className)
                    firClass.generateValueOfFunction(session, context.packageFqName, context.className)
                }

                firClass
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return withChildClassName(ANONYMOUS_OBJECT_NAME) {
                FirAnonymousObjectImpl(
                    expression.toFirSourceElement(), session, ClassKind.OBJECT, scopeProvider, FirAnonymousObjectSymbol()
                ).apply {
                    objectDeclaration.extractAnnotationsTo(this)
                    objectDeclaration.extractSuperTypeListEntriesTo(this, null, null, ClassKind.CLASS)
                    this.typeRef = superTypeRefs.first() // TODO

                    for (declaration in objectDeclaration.declarations) {
                        declarations += declaration.toFirDeclaration(
                            delegatedSuperType = null, delegatedSelfType = null,
                            owner = objectDeclaration, hasPrimaryConstructor = false
                        )
                    }
                }
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit): FirElement {
            val status = FirDeclarationStatusImpl(typeAlias.visibility, Modality.FINAL).apply {
                isExpect = typeAlias.hasExpectModifier()
                isActual = typeAlias.hasActualModifier()
            }
            return withChildClassName(typeAlias.nameAsSafeName) {
                val firTypeAlias = FirTypeAliasImpl(
                    typeAlias.toFirSourceElement(),
                    session,
                    typeAlias.nameAsSafeName,
                    status,
                    FirTypeAliasSymbol(context.currentClassId),
                    typeAlias.getTypeReference().toFirOrErrorType()
                )
                typeAlias.extractAnnotationsTo(firTypeAlias)
                typeAlias.extractTypeParametersTo(firTypeAlias)
                firTypeAlias
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
            val firFunction = if (function.name == null) {
                FirAnonymousFunctionImpl(
                    function.toFirSourceElement(), session, returnType, receiverType, FirAnonymousFunctionSymbol(), isLambda = false
                )
            } else {
                val status = FirDeclarationStatusImpl(
                    if (function.isLocal) Visibilities.LOCAL else function.visibility,
                    function.modality
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
                FirSimpleFunctionImpl(
                    function.toFirSourceElement(),
                    session,
                    returnType,
                    receiverType,
                    function.nameAsSafeName,
                    status,
                    FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName, function.isLocal))
                )
            }
            context.firFunctions += firFunction
            function.extractAnnotationsTo(firFunction)
            if (firFunction is FirSimpleFunctionImpl) {
                function.extractTypeParametersTo(firFunction)
            }
            for (valueParameter in function.valueParameters) {
                firFunction.valueParameters += valueParameter.convert<FirValueParameter>()
            }
            firFunction.body = function.buildFirBody()
            context.firFunctions.removeLast()
            return firFunction
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit): FirElement {
            val literal = expression.functionLiteral
            val literalSource = literal.toFirSourceElement()
            val returnType = FirImplicitTypeRefImpl(literalSource)
            val receiverType = FirImplicitTypeRefImpl(literalSource)
            return FirAnonymousFunctionImpl(
                literalSource, session, returnType, receiverType, FirAnonymousFunctionSymbol(), isLambda = true
            ).apply {
                context.firFunctions += this
                var destructuringBlock: FirExpression? = null
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val name = Name.special("<destruct>")
                        val multiParameter = FirValueParameterImpl(
                            valueParameter.toFirSourceElement(),
                            this@RawFirBuilder.session,
                            FirImplicitTypeRefImpl(multiDeclaration.toFirSourceElement()),
                            name,
                            FirVariableSymbol(name),
                            defaultValue = null,
                            isCrossinline = false,
                            isNoinline = false,
                            isVararg = false
                        )
                        destructuringBlock = generateDestructuringBlock(
                            this@RawFirBuilder.session,
                            multiDeclaration,
                            multiParameter,
                            tmpVariable = false,
                            extractAnnotationsTo = { extractAnnotationsTo(it) }
                        ) { toFirOrImplicitType() }
                        multiParameter
                    } else {
                        valueParameter.toFirValueParameter(FirImplicitTypeRefImpl(source))
                    }
                }
                val expressionSource = expression.toFirSourceElement()
                label = context.firLabels.pop() ?: context.firFunctionCalls.lastOrNull()?.calleeReference?.name?.let {
                    FirLabelImpl(expressionSource, it.asString())
                }
                val bodyExpression = literal.bodyExpression.toFirExpression("Lambda has no body")
                body = if (bodyExpression is FirBlockImpl) {
                    if (bodyExpression.statements.isEmpty()) {
                        bodyExpression.statements.add(FirUnitExpression(expressionSource))
                    }
                    if (destructuringBlock is FirBlock) {
                        for ((index, statement) in destructuringBlock.statements.withIndex()) {
                            bodyExpression.statements.add(index, statement)
                        }
                    }
                    bodyExpression
                } else {
                    FirSingleExpressionBlock(bodyExpression.toReturn())
                }

                context.firFunctions.removeLast()
            }
        }

        private fun KtSecondaryConstructor.toFirConstructor(
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            owner: KtClassOrObject,
            hasPrimaryConstructor: Boolean
        ): FirConstructor {
            val status = FirDeclarationStatusImpl(visibility, Modality.FINAL).apply {
                isExpect = hasExpectModifier()
                isActual = hasActualModifier()
                isInner = owner.hasModifier(INNER_KEYWORD)
            }
            val firConstructor = FirConstructorImpl(
                this.toFirSourceElement(),
                session,
                delegatedSelfTypeRef,
                null,
                status,
                FirConstructorSymbol(callableIdForClassConstructor())
            ).apply {
                delegatedConstructor = getDelegationCall().convert(delegatedSuperTypeRef, delegatedSelfTypeRef, hasPrimaryConstructor)
            }
            this@RawFirBuilder.context.firFunctions += firConstructor
            extractAnnotationsTo(firConstructor)
            firConstructor.typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
            extractValueParametersTo(firConstructor)
            firConstructor.body = buildFirBody()
            this@RawFirBuilder.context.firFunctions.removeLast()
            return firConstructor
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            hasPrimaryConstructor: Boolean
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis || (isImplicit && hasPrimaryConstructor)
            val source = this.toFirSourceElement()
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(source, FirSimpleDiagnostic("No super type", DiagnosticKind.Syntax))
            }
            return FirDelegatedConstructorCallImpl(
                source,
                delegatedType,
                isThis
            ).apply {
                if (!stubMode) {
                    extractArgumentsTo(this)
                }
            }
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return FirAnonymousInitializerImpl(
                initializer.toFirSourceElement(),
                session,
                if (stubMode) FirEmptyExpressionBlock() else initializer.body.toFirBlock()
            )
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            val propertyType = property.typeReference.toFirOrImplicitType()
            val name = property.nameAsSafeName
            val isVar = property.isVar
            val initializer = if (property.hasInitializer()) {
                { property.initializer }.toFirExpression("Should have initializer")
            } else null
            val delegateExpression by lazy { property.delegate?.expression }
            val propertySource = property.toFirSourceElement()
            val firProperty = if (property.isLocal) {
                val receiver = delegateExpression?.toFirExpression("Incorrect delegate expression")
                FirPropertyImpl(
                    propertySource,
                    session,
                    propertyType,
                    null,
                    name,
                    initializer,
                    delegateExpression?.let {
                        FirWrappedDelegateExpressionImpl(
                            it.toFirSourceElement(),
                            it.toFirExpression("Incorrect delegate expression")
                        )
                    },
                    isVar,
                    FirPropertySymbol(name),
                    true,
                    FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
                ).apply {
                    generateAccessorsByDelegate(this@RawFirBuilder.session, member = false, stubMode, receiver)
                }
            } else {
                val status = FirDeclarationStatusImpl(property.visibility, property.modality).apply {
                    isExpect = property.hasExpectModifier()
                    isActual = property.hasActualModifier()
                    isOverride = property.hasModifier(OVERRIDE_KEYWORD)
                    isConst = property.hasModifier(CONST_KEYWORD)
                    isLateInit = property.hasModifier(LATEINIT_KEYWORD)
                }
                val receiver = delegateExpression?.toFirExpression("Should have delegate")
                FirPropertyImpl(
                    propertySource,
                    session,
                    propertyType,
                    property.receiverTypeReference.convertSafe(),
                    name,
                    initializer,
                    if (property.hasDelegate()) {
                        FirWrappedDelegateExpressionImpl(
                            if (stubMode) null else delegateExpression?.toFirSourceElement(),
                            { delegateExpression }.toFirExpression("Should have delegate")
                        )
                    } else null,
                    isVar,
                    FirPropertySymbol(callableIdForName(name)),
                    false,
                    status
                ).apply {
                    property.extractTypeParametersTo(this)
                    getter = property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true)
                    setter = if (isVar) property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false) else null
                    generateAccessorsByDelegate(this@RawFirBuilder.session, member = !property.isTopLevel, stubMode, receiver)
                }
            }
            property.extractAnnotationsTo(firProperty)
            return firProperty
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val source = typeReference.toFirSourceElement()
            val isNullable = typeElement is KtNullableType

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                if (this is KtNullableType) this.innerType.unwrapNullable() else this

            val firType = when (val unwrappedElement = typeElement.unwrapNullable()) {
                is KtDynamicType -> FirDynamicTypeRefImpl(source, isNullable)
                is KtUserType -> {
                    var referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        val userType = FirUserTypeRefImpl(
                            source, isNullable
                        )
                        var qualifier: KtUserType? = unwrappedElement
                        do {
                            val firQualifier = FirQualifierPartImpl(referenceExpression!!.getReferencedNameAsName())
                            for (typeArgument in qualifier!!.typeArguments) {
                                firQualifier.typeArguments += typeArgument.convert<FirTypeProjection>()
                            }
                            userType.qualifier.add(firQualifier)

                            qualifier = qualifier.qualifier
                            referenceExpression = qualifier?.referenceExpression
                        } while (referenceExpression != null)

                        userType.qualifier.reverse()

                        userType
                    } else {
                        FirErrorTypeRefImpl(source, FirSimpleDiagnostic("Incomplete user type", DiagnosticKind.Syntax))
                    }
                }
                is KtFunctionType -> {
                    val functionType = FirFunctionTypeRefImpl(
                        source,
                        isNullable,
                        unwrappedElement.receiverTypeReference.convertSafe(),
                        // TODO: probably implicit type should not be here
                        unwrappedElement.returnTypeReference.toFirOrErrorType()
                    )
                    for (valueParameter in unwrappedElement.parameters) {
                        functionType.valueParameters += valueParameter.convert<FirValueParameter>()
                    }
                    if (functionType.receiverTypeRef != null) {
                        functionType.annotations += extensionFunctionAnnotation
                    }
                    functionType
                }
                null -> FirErrorTypeRefImpl(source, FirSimpleDiagnostic("Unwrapped type is null", DiagnosticKind.Syntax))
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (annotationEntry in typeReference.annotationEntries) {
                firType.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            return firType
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit): FirElement {
            val firAnnotationCall = FirAnnotationCallImpl(
                annotationEntry.toFirSourceElement(),
                annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget(),
                annotationEntry.typeReference.toFirOrErrorType()
            )
            annotationEntry.extractArgumentsTo(firAnnotationCall)
            return firAnnotationCall
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit): FirElement {
            val parameterName = parameter.nameAsSafeName
            val firTypeParameter = FirTypeParameterImpl(
                parameter.toFirSourceElement(),
                session,
                parameterName,
                FirTypeParameterSymbol(),
                parameter.variance,
                parameter.hasModifier(REIFIED_KEYWORD)
            )
            parameter.extractAnnotationsTo(firTypeParameter)
            val extendsBound = parameter.extendsBound
            if (extendsBound != null) {
                firTypeParameter.bounds += extendsBound.convert<FirTypeRef>()
            }
            val owner = parameter.getStrictParentOfType<KtTypeParameterListOwner>() ?: return firTypeParameter
            for (typeConstraint in owner.typeConstraints) {
                val subjectName = typeConstraint.subjectTypeParameterName?.getReferencedNameAsName()
                if (subjectName == parameterName) {
                    firTypeParameter.bounds += typeConstraint.boundTypeReference.toFirOrErrorType()
                }
            }
            firTypeParameter.addDefaultBoundIfNecessary()
            return firTypeParameter
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit): FirElement {
            val projectionKind = typeProjection.projectionKind
            val source = typeProjection.toFirSourceElement()
            if (projectionKind == KtProjectionKind.STAR) {
                return FirStarProjectionImpl(source)
            }
            if (projectionKind == KtProjectionKind.NONE && typeProjection.text == "_") {
                return FirTypePlaceholderProjection
            }
            val typeReference = typeProjection.typeReference
            val firType = typeReference.toFirOrErrorType()
            return FirTypeProjectionWithVarianceImpl(
                source,
                firType,
                when (projectionKind) {
                    KtProjectionKind.IN -> Variance.IN_VARIANCE
                    KtProjectionKind.OUT -> Variance.OUT_VARIANCE
                    KtProjectionKind.NONE -> Variance.INVARIANT
                    KtProjectionKind.STAR -> throw AssertionError("* should not be here")
                }
            )
        }

        override fun visitParameter(parameter: KtParameter, data: Unit): FirElement =
            parameter.toFirValueParameter()

        override fun visitBlockExpression(expression: KtBlockExpression, data: Unit): FirElement {
            return FirBlockImpl(expression.toFirSourceElement()).apply {
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
                ?: FirUnitExpression(source)
            return result.toReturn(source, expression.getTargetLabel()?.getReferencedName())
        }

        override fun visitTryExpression(expression: KtTryExpression, data: Unit): FirElement {
            val tryBlock = expression.tryBlock.toFirBlock()
            val finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
            return FirTryExpressionImpl(expression.toFirSourceElement(), tryBlock, finallyBlock).apply {
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.toFirValueParameter() ?: continue
                    val block = clause.catchBody.toFirBlock()
                    catches += FirCatchImpl(clause.toFirSourceElement(), parameter, block)
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression, data: Unit): FirElement {
            return FirWhenExpressionImpl(expression.toFirSourceElement(), null, null).apply {
                val condition = expression.condition
                val firCondition = condition.toFirExpression("If statement should have condition")
                val trueBranch = expression.then.toFirBlock()
                branches += FirWhenBranchImpl(condition?.toFirSourceElement(), firCondition, trueBranch)
                expression.`else`?.let {
                    branches += FirWhenBranchImpl(
                        null, FirElseIfTrueCondition(null), it.toFirBlock()
                    )
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
                    FirPropertyImpl(
                        ktSubjectExpression.toFirSourceElement(),
                        session,
                        ktSubjectExpression.typeReference.toFirOrImplicitType(),
                        null,
                        name,
                        subjectExpression,
                        null,
                        false,
                        FirPropertySymbol(name),
                        true,
                        FirDeclarationStatusImpl(Visibilities.LOCAL, Modality.FINAL)
                    )
                }
                else -> null
            }
            val hasSubject = subjectExpression != null
            val subject = FirWhenSubject()
            return FirWhenExpressionImpl(
                expression.toFirSourceElement(),
                subjectExpression,
                subjectVariable
            ).apply {
                if (hasSubject) {
                    subject.bind(this)
                }
                for (entry in expression.entries) {
                    val entrySource = entry.toFirSourceElement()
                    val branch = entry.expression.toFirBlock()
                    branches += if (!entry.isElse) {
                        if (hasSubject) {
                            val firCondition = entry.conditions.toFirWhenCondition(
                                entrySource,
                                subject,
                                { toFirExpression(it) },
                                { toFirOrErrorType() }
                            )
                            FirWhenBranchImpl(entrySource, firCondition, branch)
                        } else {
                            val condition = entry.conditions.first() as? KtWhenConditionWithExpression
                            val firCondition = condition?.expression.toFirExpression("No expression in condition with expression")
                            FirWhenBranchImpl(entrySource, firCondition, branch)
                        }
                    } else {
                        FirWhenBranchImpl(entrySource, FirElseIfTrueCondition(null), branch)
                    }
                }
            }
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Unit): FirElement {
            return FirDoWhileLoopImpl(
                expression.toFirSourceElement(), expression.condition.toFirExpression("No condition in do-while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: Unit): FirElement {
            return FirWhileLoopImpl(
                expression.toFirSourceElement(), expression.condition.toFirExpression("No condition in while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: Unit?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val parameter = expression.loopParameter
            val loopSource = expression.toFirSourceElement()
            return FirBlockImpl(loopSource).apply {
                val rangeSource = expression.loopRange?.toFirSourceElement()
                val rangeVal =
                    generateTemporaryVariable(this@RawFirBuilder.session, rangeSource, Name.special("<range>"), rangeExpression)
                statements += rangeVal
                val iteratorVal = generateTemporaryVariable(
                    this@RawFirBuilder.session, rangeSource, Name.special("<iterator>"),
                    FirFunctionCallImpl(loopSource).apply {
                        calleeReference = FirSimpleNamedReference(loopSource, Name.identifier("iterator"), null)
                        explicitReceiver = generateResolvedAccessExpression(rangeSource, rangeVal)
                    }
                )
                statements += iteratorVal
                statements += FirWhileLoopImpl(
                    loopSource,
                    FirFunctionCallImpl(loopSource).apply {
                        calleeReference = FirSimpleNamedReference(loopSource, Name.identifier("hasNext"), null)
                        explicitReceiver = generateResolvedAccessExpression(loopSource, iteratorVal)
                    }
                ).configure {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val block = when (val body = expression.body) {
                        is KtBlockExpression -> body.accept(this@Visitor, Unit) as FirBlockImpl
                        null -> FirBlockImpl(null)
                        else -> FirBlockImpl(body.toFirSourceElement()).apply { statements += body.toFirStatement() }
                    }
                    if (parameter != null) {
                        val multiDeclaration = parameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            this@RawFirBuilder.session, expression.loopParameter?.toFirSourceElement(),
                            if (multiDeclaration != null) Name.special("<destruct>") else parameter.nameAsSafeName,
                            FirFunctionCallImpl(loopSource).apply {
                                calleeReference = FirSimpleNamedReference(loopSource, Name.identifier("next"), null)
                                explicitReceiver = generateResolvedAccessExpression(loopSource, iteratorVal)
                            }
                        )
                        if (multiDeclaration != null) {
                            val destructuringBlock = generateDestructuringBlock(
                                this@RawFirBuilder.session,
                                multiDeclaration,
                                firLoopParameter,
                                tmpVariable = true,
                                extractAnnotationsTo = { extractAnnotationsTo(it) }
                            ) { toFirOrImplicitType() }
                            if (destructuringBlock is FirBlock) {
                                for ((index, statement) in destructuringBlock.statements.withIndex()) {
                                    block.statements.add(index, statement)
                                }
                            }
                        } else {
                            block.statements.add(0, firLoopParameter)
                        }
                    }
                    block
                }
            }
        }

        override fun visitBreakExpression(expression: KtBreakExpression, data: Unit): FirElement {
            return FirBreakExpressionImpl(expression.toFirSourceElement()).bindLabel(expression)
        }

        override fun visitContinueExpression(expression: KtContinueExpression, data: Unit): FirElement {
            return FirContinueExpressionImpl(expression.toFirSourceElement()).bindLabel(expression)
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")
            val source = expression.toFirSourceElement()
            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(session, rightArgument, "elvis", source)
                ANDAND, OROR ->
                    return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND, source)
                in OperatorConventions.IN_OPERATIONS ->
                    return rightArgument.generateContainsOperation(
                        leftArgument, operationToken == NOT_IN, expression, expression.operationReference
                    )
            }
            val conventionCallName = operationToken.toBinaryName()
            return if (conventionCallName != null || operationToken == IDENTIFIER) {
                FirFunctionCallImpl(source).apply {
                    calleeReference = FirSimpleNamedReference(
                        expression.operationReference.toFirSourceElement(),
                        conventionCallName ?: expression.operationReference.getReferencedNameAsName(),
                        null
                    )
                    explicitReceiver = leftArgument
                    arguments += rightArgument
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                if (firOperation in FirOperation.ASSIGNMENTS) {
                    return expression.left.generateAssignment(source, rightArgument, firOperation) {
                        (this as KtExpression).toFirExpression("Incorrect expression in assignment: ${expression.text}")
                    }
                } else {
                    FirOperatorCallImpl(source, firOperation).apply {
                        arguments += leftArgument
                        arguments += rightArgument
                    }
                }
            }
        }

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Unit): FirElement {
            val operation = expression.operationReference.getReferencedNameElementType().toFirOperation()
            return FirTypeOperatorCallImpl(
                expression.toFirSourceElement(), operation, expression.right.toFirOrErrorType()
            ).apply {
                arguments += expression.left.toFirExpression("No left operand")
            }
        }

        override fun visitIsExpression(expression: KtIsExpression, data: Unit): FirElement {
            return FirTypeOperatorCallImpl(
                expression.toFirSourceElement(), if (expression.isNegated) FirOperation.NOT_IS else FirOperation.IS,
                expression.typeReference.toFirOrErrorType()
            ).apply {
                arguments += expression.leftHandSide.toFirExpression("No left operand")
            }
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val argument = expression.baseExpression
            val conventionCallName = operationToken.toUnaryName()
            return when {
                operationToken == EXCLEXCL -> {
                    FirCheckNotNullCallImpl(expression.toFirSourceElement()).apply {
                        arguments += argument.toFirExpression("No operand")
                    }
                }
                conventionCallName != null -> {
                    if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                        return generateIncrementOrDecrementBlock(
                            expression, argument,
                            callName = conventionCallName,
                            prefix = expression is KtPrefixExpression
                        ) { (this as KtExpression).toFirExpression("Incorrect expression inside inc/dec") }
                    }
                    FirFunctionCallImpl(expression.toFirSourceElement()).apply {
                        calleeReference = FirSimpleNamedReference(
                            expression.operationReference.toFirSourceElement(), conventionCallName, null
                        )
                        explicitReceiver = argument.toFirExpression("No operand")
                    }
                }
                else -> {
                    val firOperation = operationToken.toFirOperation()
                    FirOperatorCallImpl(expression.toFirSourceElement(), firOperation).apply {
                        arguments += argument.toFirExpression("No operand")
                    }
                }
            }
        }

        private fun splitToCalleeAndReceiver(
            calleeExpression: KtExpression?,
            defaultSource: FirPsiSourceElement
        ): Pair<FirNamedReference, FirExpression?> {
            return when (calleeExpression) {
                is KtSimpleNameExpression -> FirSimpleNamedReference(
                    calleeExpression.toFirSourceElement(), calleeExpression.getReferencedNameAsName(), null
                ) to null
                is KtParenthesizedExpression -> splitToCalleeAndReceiver(calleeExpression.expression, defaultSource)
                null -> FirErrorNamedReferenceImpl(
                    null, FirSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax)
                ) to null
                else -> {
                    FirSimpleNamedReference(
                        defaultSource, OperatorNameConventions.INVOKE, null
                    ) to calleeExpression.toFirExpression("Incorrect invoke receiver")
                }
            }
        }

        override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement()
            val (calleeReference, explicitReceiver) = splitToCalleeAndReceiver(expression.calleeExpression, source)

            val result = if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
                FirQualifiedAccessExpressionImpl(source).apply {
                    this.calleeReference = calleeReference
                }
            } else {
                FirFunctionCallImpl(source).apply {
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
            }
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Unit): FirElement {
            val arrayExpression = expression.arrayExpression
            val source = expression.toFirSourceElement()
            return FirFunctionCallImpl(source).apply {
                calleeReference = FirSimpleNamedReference(source, OperatorNameConventions.GET, null)
                explicitReceiver = arrayExpression.toFirExpression("No array expression")
                for (indexExpression in expression.indexExpressions) {
                    arguments += indexExpression.toFirExpression("Incorrect index expression")
                }
            }
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Unit): FirElement {
            val selector = expression.selectorExpression
                ?: return FirErrorExpressionImpl(
                    expression.toFirSourceElement(), FirSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax)
                )
            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirModifiableQualifiedAccess) {
                firSelector.safe = expression is KtSafeQualifiedExpression
                firSelector.explicitReceiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            val labelName = expression.getLabelName()
            val source = expression.toFirSourceElement()
            return FirThisReceiverExpressionImpl(source, FirExplicitThisReference(source, labelName))
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: Unit): FirElement {
            val superType = expression.superTypeQualifier
            val source = expression.toFirSourceElement()
            return FirQualifiedAccessExpressionImpl(source).apply {
                calleeReference = FirExplicitSuperReference(source, superType.toFirOrImplicitType())
            }
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): FirElement {
            return expression.expression?.accept(this, data)
                ?: FirErrorExpressionImpl(expression.toFirSourceElement(), FirSimpleDiagnostic("Empty parentheses", DiagnosticKind.Syntax))
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): FirElement {
            val source = expression.toFirSourceElement()
            val labelName = expression.getLabelName()
            val size = context.firLabels.size
            if (labelName != null) {
                context.firLabels += FirLabelImpl(source, labelName)
            }
            val result = expression.baseExpression?.accept(this, data)
                ?: FirErrorExpressionImpl(source, FirSimpleDiagnostic("Empty label", DiagnosticKind.Syntax))
            if (size != context.firLabels.size) {
                context.firLabels.removeLast()
                println("Unused label: ${expression.text}")
            }
            return result
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Unit): FirElement {
            val rawResult = expression.baseExpression?.accept(this, data)
            val result = rawResult as? FirAbstractAnnotatedElement
                ?: FirErrorExpressionImpl(
                    expression.toFirSourceElement(),
                    FirSimpleDiagnostic("Strange annotated expression: ${rawResult?.render()}", DiagnosticKind.Syntax)
                )
            expression.extractAnnotationsTo(result)
            return result
        }

        override fun visitThrowExpression(expression: KtThrowExpression, data: Unit): FirElement {
            return FirThrowExpressionImpl(expression.toFirSourceElement(), expression.thrownExpression.toFirExpression("Nothing to throw"))
        }

        override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Unit): FirElement {
            val baseVariable = generateTemporaryVariable(
                session, multiDeclaration.toFirSourceElement(), "destruct",
                multiDeclaration.initializer.toFirExpression("Destructuring declaration without initializer")
            )
            return generateDestructuringBlock(
                session,
                multiDeclaration,
                baseVariable,
                tmpVariable = true,
                extractAnnotationsTo = { extractAnnotationsTo(it) }
            ) {
                toFirOrImplicitType()
            }
        }

        override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Unit): FirElement {
            return FirGetClassCallImpl(expression.toFirSourceElement()).apply {
                arguments += expression.receiverExpression.toFirExpression("No receiver in class literal")
            }
        }

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Unit): FirElement {
            return FirCallableReferenceAccessImpl(expression.toFirSourceElement()).apply {
                calleeReference = FirSimpleNamedReference(
                    expression.callableReference.toFirSourceElement(), expression.callableReference.getReferencedNameAsName(), null
                )
                explicitReceiver = expression.receiverExpression?.toFirExpression("Incorrect receiver expression")
            }
        }

        override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Unit): FirElement {
            return FirArrayOfCallImpl(expression.toFirSourceElement()).apply {
                for (innerExpression in expression.getInnerExpressions()) {
                    arguments += innerExpression.toFirExpression("Incorrect collection literal argument")
                }
            }
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return FirExpressionStub(expression.toFirSourceElement())
        }
    }

    private val extensionFunctionAnnotation = FirAnnotationCallImpl(
        null,
        null,
        FirResolvedTypeRefImpl(
            null,
            ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(ClassId.fromString(EXTENSION_FUNCTION_ANNOTATION)),
                emptyArray(),
                false
            )
        )
    )
}
