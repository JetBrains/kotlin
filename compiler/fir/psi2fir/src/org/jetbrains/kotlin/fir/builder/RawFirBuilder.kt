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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirWhenSubject
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.labels.FirLabelImpl
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class RawFirBuilder(session: FirSession, val stubMode: Boolean) : BaseFirBuilder<PsiElement>(session) {

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
            convertSafe() ?: FirImplicitTypeRefImpl(this)

        private fun KtTypeReference?.toFirOrUnitType(): FirTypeRef =
            convertSafe() ?: implicitUnitType

        private fun KtTypeReference?.toFirOrErrorType(): FirTypeRef =
            convertSafe() ?: FirErrorTypeRefImpl(this, if (this == null) "Incomplete code" else "Conversion failed")

        // Here we accept lambda as receiver to prevent expression calculation in stub mode
        private fun (() -> KtExpression?).toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(null)
            else with(this()) {
                convertSafe<FirExpression>() ?: FirErrorExpressionImpl(this, errorReason)
            }

        private fun KtExpression?.toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(null)
            else convertSafe<FirExpression>() ?: FirErrorExpressionImpl(this, errorReason)

        private fun KtExpression.toFirStatement(errorReason: String): FirStatement =
            convertSafe() ?: FirErrorExpressionImpl(this, errorReason)

        private fun KtExpression.toFirStatement(): FirStatement =
            convert()

        private fun KtDeclaration.toFirDeclaration(
            delegatedSuperType: FirTypeRef?, delegatedSelfType: FirTypeRef?, owner: KtClassOrObject, hasPrimaryConstructor: Boolean
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> toFirConstructor(
                    delegatedSuperType,
                    delegatedSelfType ?: FirErrorTypeRefImpl(this, "Constructor in object"),
                    owner,
                    hasPrimaryConstructor
                )
                else -> convert<FirDeclaration>()
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
                    FirSingleExpressionBlock(FirExpressionStub(this).toReturn())
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    // basePsi is null, because 'return' is synthetic & should not be bound to some PSI
                    FirSingleExpressionBlock(result.toReturn(basePsi = null))
                }
            }

        private fun ValueArgument?.toFirExpression(): FirExpression {
            this ?: return FirErrorExpressionImpl(this as? KtElement, "No argument given")
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
                name != null -> FirNamedArgumentExpressionImpl(expression, name, isSpread, firExpression)
                isSpread -> FirSpreadArgumentExpressionImpl(expression, firExpression)
                else -> firExpression
            }
        }

        private fun KtPropertyAccessor?.toFirPropertyAccessor(
            property: KtProperty,
            propertyTypeRef: FirTypeRef,
            isGetter: Boolean
        ): FirPropertyAccessor {
            if (this == null) {
                return if (isGetter) {
                    FirDefaultPropertyGetter(session, property, propertyTypeRef, property.visibility)
                } else {
                    FirDefaultPropertySetter(session, property, propertyTypeRef, property.visibility)
                }
            }
            val firAccessor = FirPropertyAccessorImpl(
                session,
                this,
                isGetter,
                visibility,
                if (isGetter) {
                    returnTypeReference?.convertSafe() ?: propertyTypeRef
                } else {
                    returnTypeReference.toFirOrUnitType()
                },
                FirPropertyAccessorSymbol()
            )
            this@RawFirBuilder.context.firFunctions += firAccessor
            extractAnnotationsTo(firAccessor)
            extractValueParametersTo(firAccessor, propertyTypeRef)
            if (!isGetter && firAccessor.valueParameters.isEmpty()) {
                firAccessor.valueParameters += FirDefaultSetterValueParameter(session, this, propertyTypeRef)
            }
            firAccessor.body = this.buildFirBody()
            this@RawFirBuilder.context.firFunctions.removeLast()
            return firAccessor
        }

        private fun KtParameter.toFirValueParameter(defaultTypeRef: FirTypeRef? = null): FirValueParameter {
            val firValueParameter = FirValueParameterImpl(
                session,
                this,
                nameAsSafeName,
                when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultTypeRef != null -> defaultTypeRef
                    else -> null.toFirOrImplicitType()
                },
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
            val firProperty = FirMemberPropertyImpl(
                session,
                this,
                FirPropertySymbol(callableIdForName(nameAsSafeName)),
                nameAsSafeName,
                visibility,
                modality,
                hasExpectModifier(),
                hasActualModifier(),
                isOverride = hasModifier(OVERRIDE_KEYWORD),
                isConst = false,
                isLateInit = false,
                receiverTypeRef = null,
                returnTypeRef = type,
                isVar = isMutable,
                initializer = FirQualifiedAccessExpressionImpl(this).apply {
                    calleeReference = FirPropertyFromParameterCallableReference(
                        this@toFirProperty, nameAsSafeName, firParameter.symbol
                    )
                },
                delegate = null
            ).apply {
                getter = FirDefaultPropertyGetter(this@RawFirBuilder.session, this@toFirProperty, type, visibility)
                setter = if (isMutable) FirDefaultPropertySetter(this@RawFirBuilder.session, this@toFirProperty, type, visibility) else null
            }
            extractAnnotationsTo(firProperty)
            return firProperty
        }

        private fun KtAnnotated.extractAnnotationsTo(container: FirAbstractAnnotatedElement) {
            for (annotationEntry in annotationEntries) {
                container.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
        }

        private fun KtTypeParameterListOwner.extractTypeParametersTo(container: FirAbstractMemberDeclaration) {
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
                    is KtLambdaArgument -> FirLambdaArgumentExpressionImpl(argument, argumentExpression)
                    else -> argumentExpression
                }
            }
        }

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(
            container: FirModifiableClass, delegatedSelfTypeRef: FirTypeRef?
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
                            type,
                            { superTypeListEntry.delegateExpression }.toFirExpression("Should have delegate")
                        )
                    }
                }
            }

            val defaultDelegatedSuperTypeRef = when {
                this is KtClass && this.isEnum() -> implicitEnumType
                this is KtClass && this.isAnnotation() -> implicitAnnotationType
                else -> implicitAnyType
            }
            // TODO: for enum / annotations, it *should* be empty
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
            val constructorCallee = superTypeCallEntry?.calleeExpression
            val firDelegatedCall = FirDelegatedConstructorCallImpl(
                constructorCallee ?: (this ?: owner),
                delegatedSuperTypeRef,
                isThis = false
            ).apply {
                if (!stubMode) {
                    superTypeCallEntry?.extractArgumentsTo(this)
                }
            }

            fun defaultVisibility() =
                if (owner is KtObjectDeclaration || owner.hasModifier(SEALED_KEYWORD) || owner.hasModifier(ENUM_KEYWORD))
                // TODO: sealed is FILE_PRIVATE (?) object / enum is HIDDEN (?)
                    Visibilities.PRIVATE
                else
                    Visibilities.UNKNOWN

            val firConstructor = FirPrimaryConstructorImpl(
                session,
                this ?: owner,
                FirConstructorSymbol(callableIdForClassConstructor()),
                this?.visibility ?: defaultVisibility(),
                this?.hasExpectModifier() ?: false,
                this?.hasActualModifier() ?: false,
                delegatedSelfTypeRef,
                firDelegatedCall
            )
            this?.extractAnnotationsTo(firConstructor)
            firConstructor.typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
            this?.extractValueParametersTo(firConstructor)
            return firConstructor
        }

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            context.packageFqName = file.packageFqName
            val firFile = FirFileImpl(session, file, file.name, context.packageFqName)
            for (annotationEntry in file.annotationEntries) {
                firFile.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            for (importDirective in file.importDirectives) {
                firFile.imports += FirImportImpl(
                    importDirective,
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

        override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit): FirElement {
            return withChildClassName(enumEntry.nameAsSafeName) {
                val firEnumEntry = FirEnumEntryImpl(
                    session,
                    enumEntry,
                    FirClassSymbol(context.currentClassId),
                    enumEntry.nameAsSafeName
                )
                enumEntry.extractAnnotationsTo(firEnumEntry)
                val delegatedSelfType = enumEntry.toDelegatedSelfType(firEnumEntry)
                val delegatedSuperType = enumEntry.extractSuperTypeListEntriesTo(firEnumEntry, delegatedSelfType)
                for (declaration in enumEntry.declarations) {
                    firEnumEntry.addDeclaration(
                        declaration.toFirDeclaration(
                            delegatedSuperType, delegatedSelfType, enumEntry, hasPrimaryConstructor = true
                        )
                    )
                }
                firEnumEntry
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
                val firClass = FirClassImpl(
                    session,
                    classOrObject,
                    FirClassSymbol(context.currentClassId),
                    classOrObject.nameAsSafeName,
                    if (classOrObject.isLocal) Visibilities.LOCAL else classOrObject.visibility,
                    classOrObject.modality,
                    classOrObject.hasExpectModifier(),
                    classOrObject.hasActualModifier(),
                    classKind,
                    isInner = classOrObject.hasModifier(INNER_KEYWORD),
                    isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true,
                    isData = (classOrObject as? KtClass)?.isData() == true,
                    isInline = classOrObject.hasModifier(INLINE_KEYWORD)
                )
                classOrObject.extractAnnotationsTo(firClass)
                classOrObject.extractTypeParametersTo(firClass)
                val delegatedSelfType = classOrObject.toDelegatedSelfType(firClass)
                val delegatedSuperType = classOrObject.extractSuperTypeListEntriesTo(firClass, delegatedSelfType)
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
                        session, firClass, context.packageFqName, context.className, firPrimaryConstructor)
                    zippedParameters.generateCopyFunction(
                        session, classOrObject, firClass, context.packageFqName, context.className, firPrimaryConstructor
                    )
                    // TODO: equals, hashCode, toString
                }

                firClass
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return FirAnonymousObjectImpl(expression).apply {
                objectDeclaration.extractAnnotationsTo(this)
                objectDeclaration.extractSuperTypeListEntriesTo(this, null)
                this.typeRef = superTypeRefs.first() // TODO

                for (declaration in objectDeclaration.declarations) {
                    declarations += declaration.toFirDeclaration(
                        delegatedSuperType = null, delegatedSelfType = null,
                        owner = objectDeclaration, hasPrimaryConstructor = false
                    )
                }
            }
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit): FirElement {
            return withChildClassName(typeAlias.nameAsSafeName) {
                val firTypeAlias = FirTypeAliasImpl(
                    session,
                    typeAlias,
                    FirTypeAliasSymbol(context.currentClassId),
                    typeAlias.nameAsSafeName,
                    typeAlias.visibility,
                    typeAlias.hasExpectModifier(),
                    typeAlias.hasActualModifier(),
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
                FirAnonymousFunctionImpl(session, function, returnType, receiverType, FirAnonymousFunctionSymbol())
            } else {

                FirMemberFunctionImpl(
                    session,
                    function,
                    FirNamedFunctionSymbol(callableIdForName(function.nameAsSafeName, function.isLocal)),
                    function.nameAsSafeName,
                    if (function.isLocal) Visibilities.LOCAL else function.visibility,
                    function.modality,
                    function.hasExpectModifier(),
                    function.hasActualModifier(),
                    function.hasModifier(OVERRIDE_KEYWORD),
                    function.hasModifier(OPERATOR_KEYWORD),
                    function.hasModifier(INFIX_KEYWORD),
                    function.hasModifier(INLINE_KEYWORD),
                    function.hasModifier(TAILREC_KEYWORD),
                    function.hasModifier(EXTERNAL_KEYWORD),
                    function.hasModifier(SUSPEND_KEYWORD),
                    receiverType,
                    returnType
                )
            }
            context.firFunctions += firFunction
            function.extractAnnotationsTo(firFunction)
            if (firFunction is FirMemberFunctionImpl) {
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
            val returnType = FirImplicitTypeRefImpl(literal)
            val receiverType = FirImplicitTypeRefImpl(literal)
            return FirAnonymousFunctionImpl(session, literal, returnType, receiverType, FirAnonymousFunctionSymbol()).apply {
                context.firFunctions += this
                var destructuringBlock: FirExpression? = null
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val multiParameter = FirValueParameterImpl(
                            this@RawFirBuilder.session, valueParameter, Name.special("<destruct>"),
                            FirImplicitTypeRefImpl(multiDeclaration),
                            defaultValue = null, isCrossinline = false, isNoinline = false, isVararg = false
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
                        valueParameter.toFirValueParameter(FirImplicitTypeRefImpl(psi))
                    }
                }
                label = context.firLabels.pop() ?: context.firFunctionCalls.lastOrNull()?.calleeReference?.name?.let {
                    FirLabelImpl(expression, it.asString())
                }
                val bodyExpression = literal.bodyExpression.toFirExpression("Lambda has no body")
                body = if (bodyExpression is FirBlockImpl) {
                    if (bodyExpression.statements.isEmpty()) {
                        bodyExpression.statements.add(FirUnitExpression(expression))
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
            val firConstructor = FirConstructorImpl(
                session,
                this,
                FirConstructorSymbol(callableIdForClassConstructor()),
                visibility,
                hasExpectModifier(),
                hasActualModifier(),
                delegatedSelfTypeRef,
                getDelegationCall().convert(delegatedSuperTypeRef, delegatedSelfTypeRef, hasPrimaryConstructor)
            )
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
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(this, "No super type")
            }
            return FirDelegatedConstructorCallImpl(
                this,
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
                session,
                initializer,
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
            val firProperty = if (property.isLocal) {
                FirVariableImpl(
                    session,
                    property,
                    name,
                    propertyType,
                    isVar,
                    initializer,
                    delegate = delegateExpression?.let {
                        FirWrappedDelegateExpressionImpl(
                            it,
                            it.toFirExpression("Incorrect delegate expression")
                        )
                    }
                ).apply {
                    generateAccessorsByDelegate(this@RawFirBuilder.session, member = false, stubMode = stubMode)
                }
            } else {
                FirMemberPropertyImpl(
                    session,
                    property,
                    FirPropertySymbol(callableIdForName(name)),
                    name,
                    property.visibility,
                    property.modality,
                    property.hasExpectModifier(),
                    property.hasActualModifier(),
                    property.hasModifier(OVERRIDE_KEYWORD),
                    property.hasModifier(CONST_KEYWORD),
                    property.hasModifier(LATEINIT_KEYWORD),
                    property.receiverTypeReference.convertSafe(),
                    propertyType,
                    isVar,
                    initializer,
                    if (property.hasDelegate()) {
                        FirWrappedDelegateExpressionImpl(
                            if (stubMode) null else delegateExpression,
                            { delegateExpression }.toFirExpression("Should have delegate")
                        )
                    } else null
                ).apply {
                    property.extractTypeParametersTo(this)
                    getter = property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true)
                    setter = if (isVar) property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false) else null
                    generateAccessorsByDelegate(this@RawFirBuilder.session, member = !property.isTopLevel, stubMode = stubMode)
                }
            }
            property.extractAnnotationsTo(firProperty)
            return firProperty
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val isNullable = typeElement is KtNullableType

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                if (this is KtNullableType) this.innerType.unwrapNullable() else this

            val firType = when (val unwrappedElement = typeElement.unwrapNullable()) {
                is KtDynamicType -> FirDynamicTypeRefImpl(typeReference, isNullable)
                is KtUserType -> {
                    var referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        val userType = FirUserTypeRefImpl(
                            typeReference, isNullable
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
                        FirErrorTypeRefImpl(typeReference, "Incomplete user type")
                    }
                }
                is KtFunctionType -> {
                    val functionType = FirFunctionTypeRefImpl(
                        typeReference,
                        isNullable,
                        unwrappedElement.receiverTypeReference.convertSafe(),
                        // TODO: probably implicit type should not be here
                        unwrappedElement.returnTypeReference.toFirOrImplicitType()
                    )
                    for (valueParameter in unwrappedElement.parameters) {
                        functionType.valueParameters += valueParameter.convert<FirValueParameter>()
                    }
                    functionType
                }
                null -> FirErrorTypeRefImpl(typeReference, "Unwrapped type is null")
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (annotationEntry in typeReference.annotationEntries) {
                firType.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            return firType
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit): FirElement {
            val firAnnotationCall = FirAnnotationCallImpl(
                annotationEntry,
                annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget(),
                annotationEntry.typeReference.toFirOrErrorType()
            )
            annotationEntry.extractArgumentsTo(firAnnotationCall)
            return firAnnotationCall
        }

        override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit): FirElement {
            val parameterName = parameter.nameAsSafeName
            val firTypeParameter = FirTypeParameterImpl(
                session,
                parameter,
                FirTypeParameterSymbol(),
                parameterName,
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
            if (projectionKind == KtProjectionKind.STAR) {
                return FirStarProjectionImpl(typeProjection)
            }
            if (projectionKind == KtProjectionKind.NONE && typeProjection.text == "_") {
                return FirTypePlaceholderProjection
            }
            val typeReference = typeProjection.typeReference
            val firType = typeReference.toFirOrErrorType()
            return FirTypeProjectionWithVarianceImpl(
                typeProjection,
                when (projectionKind) {
                    KtProjectionKind.IN -> Variance.IN_VARIANCE
                    KtProjectionKind.OUT -> Variance.OUT_VARIANCE
                    KtProjectionKind.NONE -> Variance.INVARIANT
                    KtProjectionKind.STAR -> throw AssertionError("* should not be here")
                },
                firType
            )
        }

        override fun visitParameter(parameter: KtParameter, data: Unit): FirElement =
            parameter.toFirValueParameter()

        override fun visitBlockExpression(expression: KtBlockExpression, data: Unit): FirElement {
            return FirBlockImpl(expression).apply {
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
            return generateAccessExpression(expression, expression.getReferencedNameAsName())
        }

        override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): FirElement =
            generateConstantExpressionByLiteral(expression)

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Unit): FirElement {
            return expression.entries.toInterpolatingCall(expression) {
                (this as KtStringTemplateEntryWithExpression).expression.toFirExpression(it)
            }
        }

        override fun visitReturnExpression(expression: KtReturnExpression, data: Unit): FirElement {
            val result = expression.returnedExpression?.toFirExpression("Incorrect return expression")
                ?: FirUnitExpression(expression)
            return result.toReturn(expression, expression.getTargetLabel()?.getReferencedName())
        }

        override fun visitTryExpression(expression: KtTryExpression, data: Unit): FirElement {
            val tryBlock = expression.tryBlock.toFirBlock()
            val finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
            return FirTryExpressionImpl(expression, tryBlock, finallyBlock).apply {
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.toFirValueParameter() ?: continue
                    val block = clause.catchBody.toFirBlock()
                    catches += FirCatchImpl(clause, parameter, block)
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression, data: Unit): FirElement {
            return FirWhenExpressionImpl(expression).apply {
                val condition = expression.condition
                val firCondition = condition.toFirExpression("If statement should have condition")
                val trueBranch = expression.then.toFirBlock()
                branches += FirWhenBranchImpl(condition, firCondition, trueBranch)
                val elseBranch = expression.`else`.toFirBlock()
                branches += FirWhenBranchImpl(
                    null, FirElseIfTrueCondition(null), elseBranch
                )
            }
        }

        override fun visitWhenExpression(expression: KtWhenExpression, data: Unit): FirElement {
            val ktSubjectExpression = expression.subjectExpression
            val subjectExpression = when (ktSubjectExpression) {
                is KtVariableDeclaration -> ktSubjectExpression.initializer
                else -> ktSubjectExpression
            }?.toFirExpression("Incorrect when subject expression: ${ktSubjectExpression?.text}")
            val subjectVariable = when (ktSubjectExpression) {
                is KtVariableDeclaration -> FirVariableImpl(
                    session, ktSubjectExpression, ktSubjectExpression.nameAsSafeName,
                    ktSubjectExpression.typeReference.toFirOrImplicitType(),
                    isVar = false, initializer = subjectExpression
                )
                else -> null
            }
            val hasSubject = subjectExpression != null
            val subject = FirWhenSubject()
            return FirWhenExpressionImpl(
                expression,
                subjectExpression,
                subjectVariable
            ).apply {
                if (hasSubject) {
                    subject.bind(this)
                }
                for (entry in expression.entries) {
                    val branch = entry.expression.toFirBlock()
                    branches += if (!entry.isElse) {
                        if (hasSubject) {
                            val firCondition = entry.conditions.toFirWhenCondition(
                                entry,
                                subject,
                                { toFirExpression(it) },
                                { toFirOrErrorType() }
                            )
                            FirWhenBranchImpl(entry, firCondition, branch)
                        } else {
                            val condition = entry.conditions.first() as? KtWhenConditionWithExpression
                            val firCondition = condition?.expression.toFirExpression("No expression in condition with expression")
                            FirWhenBranchImpl(entry, firCondition, branch)
                        }
                    } else {
                        FirWhenBranchImpl(entry, FirElseIfTrueCondition(null), branch)
                    }
                }
            }
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Unit): FirElement {
            return FirDoWhileLoopImpl(
                expression, expression.condition.toFirExpression("No condition in do-while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: Unit): FirElement {
            return FirWhileLoopImpl(
                expression, expression.condition.toFirExpression("No condition in while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: Unit?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val parameter = expression.loopParameter
            return FirBlockImpl(expression).apply {
                val rangeVal =
                    generateTemporaryVariable(this@RawFirBuilder.session, expression.loopRange, Name.special("<range>"), rangeExpression)
                statements += rangeVal
                val iteratorVal = generateTemporaryVariable(
                    this@RawFirBuilder.session, expression.loopRange, Name.special("<iterator>"),
                    FirFunctionCallImpl(expression).apply {
                        calleeReference = FirSimpleNamedReference(expression, Name.identifier("iterator"))
                        explicitReceiver = generateResolvedAccessExpression(expression.loopRange, rangeVal)
                    }
                )
                statements += iteratorVal
                statements += FirWhileLoopImpl(
                    expression,
                    FirFunctionCallImpl(expression).apply {
                        calleeReference = FirSimpleNamedReference(expression, Name.identifier("hasNext"))
                        explicitReceiver = generateResolvedAccessExpression(expression, iteratorVal)
                    }
                ).configure {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val block = when (val body = expression.body) {
                        is KtBlockExpression -> body.accept(this@Visitor, Unit) as FirBlockImpl
                        null -> FirBlockImpl(body)
                        else -> FirBlockImpl(body).apply { statements += body.toFirStatement() }
                    }
                    if (parameter != null) {
                        val multiDeclaration = parameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            this@RawFirBuilder.session, expression.loopParameter,
                            if (multiDeclaration != null) Name.special("<destruct>") else parameter.nameAsSafeName,
                            FirFunctionCallImpl(expression).apply {
                                calleeReference = FirSimpleNamedReference(expression, Name.identifier("next"))
                                explicitReceiver = generateResolvedAccessExpression(expression, iteratorVal)
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
            return FirBreakExpressionImpl(expression).bindLabel(expression)
        }

        override fun visitContinueExpression(expression: KtContinueExpression, data: Unit): FirElement {
            return FirContinueExpressionImpl(expression).bindLabel(expression)
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")
            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(session, rightArgument, "elvis", expression)
                ANDAND, OROR ->
                    return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND, expression)
                in OperatorConventions.IN_OPERATIONS ->
                    return rightArgument.generateContainsOperation(
                        leftArgument, operationToken == NOT_IN, expression, expression.operationReference
                    )
            }
            val conventionCallName = operationToken.toBinaryName()
            return if (conventionCallName != null || operationToken == IDENTIFIER) {
                FirFunctionCallImpl(expression).apply {
                    calleeReference = FirSimpleNamedReference(
                        expression.operationReference,
                        conventionCallName ?: expression.operationReference.getReferencedNameAsName()
                    )
                    explicitReceiver = leftArgument
                    arguments += rightArgument
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                if (firOperation in FirOperation.ASSIGNMENTS) {
                    return expression.left.generateAssignment(expression, rightArgument, firOperation) {
                        (this as KtExpression).toFirExpression("Incorrect expression in assignment: ${expression.text}")
                    }
                } else {
                    FirOperatorCallImpl(expression, firOperation).apply {
                        arguments += leftArgument
                        arguments += rightArgument
                    }
                }
            }
        }

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Unit): FirElement {
            val operation = expression.operationReference.getReferencedNameElementType().toFirOperation()
            return FirTypeOperatorCallImpl(
                expression, operation, expression.right.toFirOrErrorType()
            ).apply {
                arguments += expression.left.toFirExpression("No left operand")
            }
        }

        override fun visitIsExpression(expression: KtIsExpression, data: Unit): FirElement {
            return FirTypeOperatorCallImpl(
                expression, if (expression.isNegated) FirOperation.NOT_IS else FirOperation.IS,
                expression.typeReference.toFirOrErrorType()
            ).apply {
                arguments += expression.leftHandSide.toFirExpression("No left operand")
            }
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val argument = expression.baseExpression
            if (operationToken == EXCLEXCL) {
                return expression.baseExpression.bangBangToWhen(expression) { (this as KtExpression).toFirExpression(it) }
            }
            val conventionCallName = operationToken.toUnaryName()
            return if (conventionCallName != null) {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        expression, argument,
                        callName = conventionCallName,
                        prefix = expression is KtPrefixExpression
                    ) { (this as KtExpression).toFirExpression("Incorrect expression inside inc/dec") }
                }
                FirFunctionCallImpl(expression).apply {
                    calleeReference = FirSimpleNamedReference(
                        expression.operationReference, conventionCallName
                    )
                    explicitReceiver = argument.toFirExpression("No operand")
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                FirOperatorCallImpl(expression, firOperation).apply {
                    arguments += argument.toFirExpression("No operand")
                }
            }
        }

        override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
            val calleeExpression = expression.calleeExpression
            return FirFunctionCallImpl(expression).apply {
                val calleeReference = when (calleeExpression) {
                    is KtSimpleNameExpression -> FirSimpleNamedReference(
                        calleeExpression, calleeExpression.getReferencedNameAsName()
                    )
                    null -> FirErrorNamedReference(
                        calleeExpression, "Call has no callee"
                    )
                    else -> {
                        arguments += calleeExpression.toFirExpression("Incorrect invoke receiver")
                        FirSimpleNamedReference(
                            expression, OperatorNameConventions.INVOKE
                        )
                    }
                }
                this.calleeReference = calleeReference
                context.firFunctionCalls += this
                expression.extractArgumentsTo(this)
                for (typeArgument in expression.typeArguments) {
                    typeArguments += typeArgument.convert<FirTypeProjection>()
                }
                context.firFunctionCalls.removeLast()
            }
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Unit): FirElement {
            val arrayExpression = expression.arrayExpression
            return FirFunctionCallImpl(expression).apply {
                calleeReference = FirSimpleNamedReference(expression, OperatorNameConventions.GET)
                explicitReceiver = arrayExpression.toFirExpression("No array expression")
                for (indexExpression in expression.indexExpressions) {
                    arguments += indexExpression.toFirExpression("Incorrect index expression")
                }
            }
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Unit): FirElement {
            val selector = expression.selectorExpression
                ?: return FirErrorExpressionImpl(expression, "Qualified expression without selector")
            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirModifiableQualifiedAccess<*>) {
                firSelector.safe = expression is KtSafeQualifiedExpression
                firSelector.explicitReceiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            val labelName = expression.getLabelName()
            return FirThisReceiverExpressionImpl(expression, FirExplicitThisReference(expression, labelName))
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: Unit): FirElement {
            val superType = expression.superTypeQualifier
            return FirQualifiedAccessExpressionImpl(expression).apply {
                calleeReference = FirExplicitSuperReference(expression, superType.toFirOrImplicitType())
            }
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): FirElement {
            return expression.expression?.accept(this, data) ?: FirErrorExpressionImpl(expression, "Empty parentheses")
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): FirElement {
            val labelName = expression.getLabelName()
            val size = context.firLabels.size
            if (labelName != null) {
                context.firLabels += FirLabelImpl(expression, labelName)
            }
            val result = expression.baseExpression?.accept(this, data) ?: FirErrorExpressionImpl(expression, "Empty label")
            if (size != context.firLabels.size) {
                context.firLabels.removeLast()
                println("Unused label: ${expression.text}")
            }
            return result
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Unit): FirElement {
            val rawResult = expression.baseExpression?.accept(this, data)
            val result = rawResult as? FirAbstractAnnotatedElement
                ?: FirErrorExpressionImpl(expression, "Strange annotated expression: ${rawResult?.render()}")
            expression.extractAnnotationsTo(result)
            return result
        }

        override fun visitThrowExpression(expression: KtThrowExpression, data: Unit): FirElement {
            return FirThrowExpressionImpl(expression, expression.thrownExpression.toFirExpression("Nothing to throw"))
        }

        override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Unit): FirElement {
            val baseVariable = generateTemporaryVariable(
                session, multiDeclaration, "destruct",
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
            return FirGetClassCallImpl(expression).apply {
                arguments += expression.receiverExpression.toFirExpression("No receiver in class literal")
            }
        }

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Unit): FirElement {
            return FirCallableReferenceAccessImpl(expression).apply {
                calleeReference = FirSimpleNamedReference(
                    expression.callableReference, expression.callableReference.getReferencedNameAsName()
                )
                explicitReceiver = expression.receiverExpression?.toFirExpression("Incorrect receiver expression")
            }
        }

        override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Unit): FirElement {
            return FirArrayOfCallImpl(expression).apply {
                for (innerExpression in expression.getInnerExpressions()) {
                    arguments += innerExpression.toFirExpression("Incorrect collection literal argument")
                }
            }
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return FirExpressionStub(expression)
        }
    }
}