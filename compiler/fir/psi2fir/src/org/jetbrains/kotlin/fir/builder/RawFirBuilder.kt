/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.labels.FirLabelImpl
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class RawFirBuilder(val session: FirSession, val stubMode: Boolean) {

    private val implicitUnitType = FirImplicitUnitTypeRef(session, null)

    private val implicitAnyType = FirImplicitAnyTypeRef(session, null)

    private val implicitEnumType = FirImplicitEnumTypeRef(session, null)

    private val implicitAnnotationType = FirImplicitAnnotationTypeRef(session, null)

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

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
            convertSafe() ?: FirImplicitTypeRefImpl(session, this)

        private fun KtTypeReference?.toFirOrUnitType(): FirTypeRef =
            convertSafe() ?: implicitUnitType

        private fun KtTypeReference?.toFirOrErrorType(): FirTypeRef =
            convertSafe() ?: FirErrorTypeRefImpl(session, this, if (this == null) "Incomplete code" else "Conversion failed")

        // Here we accept lambda as receiver to prevent expression calculation in stub mode
        private fun (() -> KtExpression?).toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(session, null)
            else with(this()) {
                convertSafe<FirExpression>() ?: FirErrorExpressionImpl(session, this, errorReason)
            }

        private fun KtExpression?.toFirExpression(errorReason: String): FirExpression =
            if (stubMode) FirExpressionStub(session, null)
            else convertSafe<FirExpression>() ?: FirErrorExpressionImpl(session, this, errorReason)

        private fun KtExpression.toFirStatement(errorReason: String): FirStatement =
            convertSafe() ?: FirErrorExpressionImpl(session, this, errorReason)

        private fun KtExpression.toFirStatement(): FirStatement =
            convert()

        private fun KtDeclaration.toFirDeclaration(
            delegatedSuperType: FirTypeRef?, delegatedSelfType: FirTypeRef?, owner: KtClassOrObject, hasPrimaryConstructor: Boolean
        ): FirDeclaration {
            return when (this) {
                is KtSecondaryConstructor -> toFirConstructor(
                    delegatedSuperType,
                    delegatedSelfType ?: FirErrorTypeRefImpl(this@RawFirBuilder.session, this, "Constructor in object"),
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
                    FirEmptyExpressionBlock(session)
                else ->
                    FirSingleExpressionBlock(
                        session,
                        convert()
                    )
            }

        private fun FirExpression.toReturn(basePsi: PsiElement? = psi, labelName: String? = null): FirReturnExpression {
            return FirReturnExpressionImpl(
                this@RawFirBuilder.session,
                basePsi,
                this
            ).apply {
                target = FirFunctionTarget(labelName)
                val lastFunction = firFunctions.lastOrNull()
                if (labelName == null) {
                    if (lastFunction != null) {
                        target.bind(lastFunction)
                    } else {
                        target.bind(FirErrorFunction(this@RawFirBuilder.session, psi, "Cannot bind unlabeled return to a function"))
                    }
                } else {
                    for (firFunction in firFunctions.asReversed()) {
                        when (firFunction) {
                            is FirAnonymousFunction -> {
                                if (firFunction.label?.name == labelName) {
                                    target.bind(firFunction)
                                    return@apply
                                }
                            }
                            is FirNamedFunction -> {
                                if (firFunction.name.asString() == labelName) {
                                    target.bind(firFunction)
                                    return@apply
                                }
                            }
                        }
                    }
                    target.bind(FirErrorFunction(this@RawFirBuilder.session, psi, "Cannot bind label $labelName to a function"))
                }
            }
        }

        private fun KtDeclarationWithBody.buildFirBody(): FirBlock? =
            when {
                !hasBody() ->
                    null
                hasBlockBody() -> if (!stubMode) {
                    bodyBlockExpression?.accept(this@Visitor, Unit) as? FirBlock
                } else {
                    FirSingleExpressionBlock(
                        session,
                        FirExpressionStub(session, this).toReturn()
                    )
                }
                else -> {
                    val result = { bodyExpression }.toFirExpression("Function has no body (but should)")
                    FirSingleExpressionBlock(
                        session,
                        result.toReturn()
                    )
                }
            }

        private fun ValueArgument?.toFirExpression(): FirExpression {
            this ?: return FirErrorExpressionImpl(session, this as? KtElement, "No argument given")
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
            return if (name != null) FirNamedArgumentExpressionImpl(session, expression, name, firExpression) else firExpression
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
                }
            )
            firFunctions += firAccessor
            extractAnnotationsTo(firAccessor)
            extractValueParametersTo(firAccessor, propertyTypeRef)
            if (!isGetter && firAccessor.valueParameters.isEmpty()) {
                firAccessor.valueParameters += FirDefaultSetterValueParameter(session, this, propertyTypeRef)
            }
            firAccessor.body = this.buildFirBody()
            firFunctions.removeLast()
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
                initializer = FirQualifiedAccessExpressionImpl(session, this).apply {
                    calleeReference = FirPropertyFromParameterCallableReference(
                        this@RawFirBuilder.session, this@toFirProperty, nameAsSafeName, firParameter.symbol
                    )
                },
                getter = FirDefaultPropertyGetter(session, this, type, visibility),
                setter = FirDefaultPropertySetter(session, this, type, visibility),
                delegate = null
            )
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
            container: FirFunction,
            defaultTypeRef: FirTypeRef? = null
        ) {
            for (valueParameter in valueParameters) {
                (container.valueParameters as MutableList<FirValueParameter>) += valueParameter.toFirValueParameter(defaultTypeRef)
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirAbstractCall) {
            for (argument in this.valueArguments) {
                val argumentExpression = argument.toFirExpression()
                container.arguments += when (argument) {
                    is KtLambdaArgument -> FirLambdaArgumentExpressionImpl(session, argument, argumentExpression)
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
                            session, type,
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
                session,
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
                FirFunctionSymbol(callableIdForClassConstructor()),
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

        lateinit var packageFqName: FqName

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            packageFqName = file.packageFqName
            val firFile = FirFileImpl(session, file, file.name, packageFqName)
            for (annotationEntry in file.annotationEntries) {
                firFile.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            for (importDirective in file.importDirectives) {
                firFile.imports += FirImportImpl(
                    session,
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

        private fun KtClassOrObject.toDelegatedSelfType(firClass: FirRegularClass): FirTypeRef {
            val typeParameters = firClass.typeParameters.map {
                FirTypeParameterImpl(session, it.psi, FirTypeParameterSymbol(), it.name, Variance.INVARIANT, false).apply {
                    this.bounds += it.bounds
                }
            }
            return FirResolvedTypeRefImpl(
                session,
                this,
                ConeClassTypeImpl(
                    firClass.symbol.toLookupTag(),
                    typeParameters.map { ConeTypeParameterTypeImpl(it.symbol, false) }.toTypedArray(),
                    false
                )
            )
        }



        override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit): FirElement {
            return withChildClassName(enumEntry.nameAsSafeName) {
                val firEnumEntry = FirEnumEntryImpl(
                    session,
                    enumEntry,
                    FirClassSymbol(currentClassId),
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

        inline fun <T> withChildClassName(name: Name, l: () -> T): T {
            className = className.child(name)
            val t = l()
            className = className.parent()
            return t
        }

        val currentClassId get() = ClassId(packageFqName, className, false)

        fun callableIdForName(name: Name, local: Boolean = false) =
            when {
                local -> CallableId(name)
                className == FqName.ROOT -> CallableId(packageFqName, name)
                else -> CallableId(packageFqName, className, name)
            }

        fun callableIdForClassConstructor() =
            if (className == FqName.ROOT) CallableId(packageFqName, Name.special("<anonymous-init>"))
            else CallableId(packageFqName, className, className.shortName())

        var className: FqName = FqName.ROOT

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
                    FirClassSymbol(currentClassId),
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

                if (classOrObject.hasModifier(KtTokens.DATA_KEYWORD) && firPrimaryConstructor != null) {
                    var componentIndex = 1
                    val zippedParameters =
                        classOrObject.primaryConstructorParameters.zip(firClass.declarations.filterIsInstance<FirProperty>())
                    for ((ktParameter, firProperty) in zippedParameters) {
                        if (!ktParameter.hasValOrVar()) continue
                        val name = Name.identifier("component$componentIndex")
                        componentIndex++
                        val symbol = FirFunctionSymbol(callableIdForName(name))
                        firClass.addDeclaration(
                            FirMemberFunctionImpl(
                                session, ktParameter, symbol, name,
                                Visibilities.PUBLIC, Modality.FINAL,
                                isExpect = false, isActual = false,
                                isOverride = false, isOperator = false,
                                isInfix = false, isInline = false,
                                isTailRec = false, isExternal = false,
                                isSuspend = false, receiverTypeRef = null,
                                returnTypeRef = FirImplicitTypeRefImpl(session, ktParameter)
                            ).apply {
                                val componentFunction = this
                                body = FirSingleExpressionBlock(
                                    this@RawFirBuilder.session,
                                    FirReturnExpressionImpl(
                                        this@RawFirBuilder.session, ktParameter,
                                        FirQualifiedAccessExpressionImpl(this@RawFirBuilder.session, ktParameter).apply {
                                            val parameterName = ktParameter.nameAsSafeName
                                            calleeReference = FirResolvedCallableReferenceImpl(
                                                this@RawFirBuilder.session, ktParameter,
                                                parameterName, firProperty.symbol
                                            )
                                        }
                                    ).apply {
                                        target = FirFunctionTarget(null)
                                        target.bind(componentFunction)
                                    }
                                )
                            }
                        )
                    }
                }

                firClass
            }
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Unit): FirElement {
            val objectDeclaration = expression.objectDeclaration
            return FirAnonymousObjectImpl(session, expression).apply {
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
                    FirTypeAliasSymbol(currentClassId),
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

        private val firFunctions = mutableListOf<FirFunction>()

        private fun <T> MutableList<T>.removeLast() {
            removeAt(size - 1)
        }

        private fun <T> MutableList<T>.pop(): T? {
            val result = lastOrNull()
            if (result != null) {
                removeAt(size - 1)
            }
            return result
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
                FirAnonymousFunctionImpl(session, function, returnType, receiverType)
            } else {

                FirMemberFunctionImpl(
                    session,
                    function,
                    FirFunctionSymbol(callableIdForName(function.nameAsSafeName, function.isLocal)),
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
            firFunctions += firFunction
            function.extractAnnotationsTo(firFunction)
            if (firFunction is FirMemberFunctionImpl) {
                function.extractTypeParametersTo(firFunction)
            }
            for (valueParameter in function.valueParameters) {
                firFunction.valueParameters += valueParameter.convert<FirValueParameter>()
            }
            firFunction.body = function.buildFirBody()
            firFunctions.removeLast()
            return firFunction
        }

        override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit): FirElement {
            val literal = expression.functionLiteral
            val returnType = FirImplicitTypeRefImpl(session, literal)
            val receiverType = FirImplicitTypeRefImpl(session, literal)
            return FirAnonymousFunctionImpl(session, literal, returnType, receiverType).apply {
                firFunctions += this
                var destructuringBlock: FirExpression? = null
                for (valueParameter in literal.valueParameters) {
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    valueParameters += if (multiDeclaration != null) {
                        val multiParameter = FirValueParameterImpl(
                            this@RawFirBuilder.session, valueParameter, Name.special("<destruct>"),
                            FirImplicitTypeRefImpl(this@RawFirBuilder.session, multiDeclaration),
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
                        valueParameter.toFirValueParameter(FirImplicitTypeRefImpl(session, psi))
                    }
                }
                label = firLabels.pop() ?: firFunctionCalls.lastOrNull()?.calleeReference?.name?.let {
                    FirLabelImpl(this@RawFirBuilder.session, expression, it.asString())
                }
                val bodyExpression = literal.bodyExpression.toFirExpression("Lambda has no body")
                body = if (bodyExpression is FirBlockImpl) {
                    if (bodyExpression.statements.isEmpty()) {
                        bodyExpression.statements.add(FirUnitExpression(this@RawFirBuilder.session, expression))
                    }
                    if (destructuringBlock is FirBlock) {
                        for ((index, statement) in destructuringBlock.statements.withIndex()) {
                            bodyExpression.statements.add(index, statement)
                        }
                    }
                    bodyExpression
                } else {
                    FirSingleExpressionBlock(this@RawFirBuilder.session, bodyExpression.toReturn())
                }

                firFunctions.removeLast()
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
                FirFunctionSymbol(callableIdForClassConstructor()),
                visibility,
                hasExpectModifier(),
                hasActualModifier(),
                delegatedSelfTypeRef,
                getDelegationCall().convert(delegatedSuperTypeRef, delegatedSelfTypeRef, hasPrimaryConstructor)
            )
            firFunctions += firConstructor
            extractAnnotationsTo(firConstructor)
            firConstructor.typeParameters += typeParametersFromSelfType(delegatedSelfTypeRef)
            extractValueParametersTo(firConstructor)
            firConstructor.body = buildFirBody()
            firFunctions.removeLast()
            return firConstructor
        }

        private fun typeParametersFromSelfType(delegatedSelfTypeRef: FirTypeRef): List<FirTypeParameter> {
            return delegatedSelfTypeRef.coneTypeSafe()
                ?.typeArguments
                ?.map { ((it as ConeTypeParameterType).lookupTag as FirTypeParameterSymbol).fir }
                ?: emptyList()
        }

        private fun KtConstructorDelegationCall.convert(
            delegatedSuperTypeRef: FirTypeRef?,
            delegatedSelfTypeRef: FirTypeRef,
            hasPrimaryConstructor: Boolean
        ): FirDelegatedConstructorCall {
            val isThis = isCallToThis || (isImplicit && hasPrimaryConstructor)
            val delegatedType = when {
                isThis -> delegatedSelfTypeRef
                else -> delegatedSuperTypeRef ?: FirErrorTypeRefImpl(session, this, "No super type")
            }
            return FirDelegatedConstructorCallImpl(
                session,
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
                if (stubMode) FirEmptyExpressionBlock(session) else initializer.body.toFirBlock()
            )
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            val propertyType = property.typeReference.toFirOrImplicitType()
            val name = property.nameAsSafeName
            val isVar = property.isVar
            val initializer = if (property.hasInitializer()) {
                { property.initializer }.toFirExpression("Should have initializer")
            } else null
            val firProperty = if (property.isLocal) {
                FirVariableImpl(
                    session,
                    property,
                    name,
                    propertyType,
                    isVar,
                    initializer,
                    delegate = property.delegate?.expression?.toFirExpression("Incorrect delegate expression")
                )
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
                    property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true),
                    property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false),
                    if (property.hasDelegate()) {
                        { property.delegate?.expression }.toFirExpression("Should have delegate")
                    } else null
                ).apply {
                    property.extractTypeParametersTo(this)
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
                is KtDynamicType -> FirDynamicTypeRefImpl(session, typeReference, isNullable)
                is KtUserType -> {
                    var referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        val userType = FirUserTypeRefImpl(
                            session, typeReference, isNullable
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
                        FirErrorTypeRefImpl(session, typeReference, "Incomplete user type")
                    }
                }
                is KtFunctionType -> {
                    val functionType = FirFunctionTypeRefImpl(
                        session,
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
                null -> FirErrorTypeRefImpl(session, typeReference, "Unwrapped type is null")
                else -> throw AssertionError("Unexpected type element: ${unwrappedElement.text}")
            }

            for (annotationEntry in typeReference.annotationEntries) {
                firType.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            return firType
        }

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit): FirElement {
            val firAnnotationCall = FirAnnotationCallImpl(
                session,
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
            return firTypeParameter
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit): FirElement {
            val projectionKind = typeProjection.projectionKind
            if (projectionKind == KtProjectionKind.STAR) {
                return FirStarProjectionImpl(session, typeProjection)
            }
            if (projectionKind == KtProjectionKind.NONE && typeProjection.text == "_") {
                return FirTypePlaceholderProjection
            }
            val typeReference = typeProjection.typeReference
            val firType = typeReference.toFirOrErrorType()
            return FirTypeProjectionWithVarianceImpl(
                session,
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
            return FirBlockImpl(session, expression).apply {
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
            return generateAccessExpression(session, expression, expression.getReferencedNameAsName())
        }

        override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): FirElement =
            generateConstantExpressionByLiteral(session, expression)

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Unit): FirElement {
            return expression.entries.toInterpolatingCall(session, expression) { toFirExpression(it) }
        }

        override fun visitReturnExpression(expression: KtReturnExpression, data: Unit): FirElement {
            val result = expression.returnedExpression?.toFirExpression("Incorrect return expression")
                ?: FirUnitExpression(session, expression)
            return result.toReturn(expression, expression.getTargetLabel()?.getReferencedName())
        }

        override fun visitTryExpression(expression: KtTryExpression, data: Unit): FirElement {
            val tryBlock = expression.tryBlock.toFirBlock()
            val finallyBlock = expression.finallyBlock?.finalExpression?.toFirBlock()
            return FirTryExpressionImpl(session, expression, tryBlock, finallyBlock).apply {
                for (clause in expression.catchClauses) {
                    val parameter = clause.catchParameter?.toFirValueParameter() ?: continue
                    val block = clause.catchBody.toFirBlock()
                    catches += FirCatchImpl(this@RawFirBuilder.session, clause, parameter, block)
                }
            }
        }

        override fun visitIfExpression(expression: KtIfExpression, data: Unit): FirElement {
            return FirWhenExpressionImpl(
                session,
                expression
            ).apply {
                val condition = expression.condition
                val firCondition = condition.toFirExpression("If statement should have condition")
                val trueBranch = expression.then.toFirBlock()
                branches += FirWhenBranchImpl(this@RawFirBuilder.session, condition, firCondition, trueBranch)
                val elseBranch = expression.`else`.toFirBlock()
                branches += FirWhenBranchImpl(
                    this@RawFirBuilder.session, null, FirElseIfTrueCondition(this@RawFirBuilder.session, null), elseBranch
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
                session,
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
                                this@RawFirBuilder.session,
                                entry,
                                subject,
                                { toFirExpression(it) },
                                { toFirOrErrorType() }
                            )
                            FirWhenBranchImpl(this@RawFirBuilder.session, entry, firCondition, branch)
                        } else {
                            val condition = entry.conditions.first() as? KtWhenConditionWithExpression
                            val firCondition = condition?.expression.toFirExpression("No expression in condition with expression")
                            FirWhenBranchImpl(this@RawFirBuilder.session, entry, firCondition, branch)
                        }
                    } else {
                        FirWhenBranchImpl(
                            this@RawFirBuilder.session, entry, FirElseIfTrueCondition(this@RawFirBuilder.session, null), branch
                        )
                    }
                }
            }
        }

        private val firLoops = mutableListOf<FirLoop>()

        private fun FirAbstractLoop.configure(generateBlock: () -> FirBlock): FirAbstractLoop {
            label = firLabels.pop()
            firLoops += this
            block = generateBlock()
            firLoops.removeLast()
            return this
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Unit): FirElement {
            return FirDoWhileLoopImpl(
                session, expression, expression.condition.toFirExpression("No condition in do-while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitWhileExpression(expression: KtWhileExpression, data: Unit): FirElement {
            return FirWhileLoopImpl(
                session, expression, expression.condition.toFirExpression("No condition in while loop")
            ).configure { expression.body.toFirBlock() }
        }

        override fun visitForExpression(expression: KtForExpression, data: Unit?): FirElement {
            val rangeExpression = expression.loopRange.toFirExpression("No range in for loop")
            val parameter = expression.loopParameter
            return FirBlockImpl(session, expression).apply {
                val rangeVal =
                    generateTemporaryVariable(this@RawFirBuilder.session, expression.loopRange, Name.special("<range>"), rangeExpression)
                statements += rangeVal
                val iteratorVal = generateTemporaryVariable(
                    this@RawFirBuilder.session, expression.loopRange, Name.special("<iterator>"),
                    FirFunctionCallImpl(this@RawFirBuilder.session, expression).apply {
                        calleeReference = FirSimpleNamedReference(this@RawFirBuilder.session, expression, Name.identifier("iterator"))
                        explicitReceiver = generateResolvedAccessExpression(this@RawFirBuilder.session, expression.loopRange, rangeVal)
                    }
                )
                statements += iteratorVal
                statements += FirWhileLoopImpl(
                    this@RawFirBuilder.session, expression,
                    FirFunctionCallImpl(this@RawFirBuilder.session, expression).apply {
                        calleeReference = FirSimpleNamedReference(this@RawFirBuilder.session, expression, Name.identifier("hasNext"))
                        explicitReceiver = generateResolvedAccessExpression(this@RawFirBuilder.session, expression, iteratorVal)
                    }
                ).configure {
                    // NB: just body.toFirBlock() isn't acceptable here because we need to add some statements
                    val block = when (val body = expression.body) {
                        is KtBlockExpression -> body.accept(this@Visitor, Unit) as FirBlockImpl
                        null -> FirBlockImpl(this@RawFirBuilder.session, body)
                        else -> FirBlockImpl(this@RawFirBuilder.session, body).apply { statements += body.toFirStatement() }
                    }
                    if (parameter != null) {
                        val multiDeclaration = parameter.destructuringDeclaration
                        val firLoopParameter = generateTemporaryVariable(
                            this@RawFirBuilder.session, expression.loopParameter,
                            if (multiDeclaration != null) Name.special("<destruct>") else parameter.nameAsSafeName,
                            FirFunctionCallImpl(this@RawFirBuilder.session, expression).apply {
                                calleeReference = FirSimpleNamedReference(this@RawFirBuilder.session, expression, Name.identifier("next"))
                                explicitReceiver = generateResolvedAccessExpression(this@RawFirBuilder.session, expression, iteratorVal)
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

        private fun FirAbstractLoopJump.bindLabel(expression: KtExpressionWithLabel): FirAbstractLoopJump {
            val labelName = expression.getLabelName()
            target = FirLoopTarget(labelName)
            val lastLoop = firLoops.lastOrNull()
            if (labelName == null) {
                if (lastLoop != null) {
                    target.bind(lastLoop)
                } else {
                    target.bind(FirErrorLoop(this@RawFirBuilder.session, psi, "Cannot bind unlabeled jump to a loop"))
                }
            } else {
                for (firLoop in firLoops.asReversed()) {
                    if (firLoop.label?.name == labelName) {
                        target.bind(firLoop)
                        return this
                    }
                }
                target.bind(FirErrorLoop(this@RawFirBuilder.session, psi, "Cannot bind label $labelName to a loop"))
            }
            return this
        }

        override fun visitBreakExpression(expression: KtBreakExpression, data: Unit): FirElement {
            return FirBreakExpressionImpl(session, expression).bindLabel(expression)
        }

        override fun visitContinueExpression(expression: KtContinueExpression, data: Unit): FirElement {
            return FirContinueExpressionImpl(session, expression).bindLabel(expression)
        }

        private fun KtUnaryExpression.bangBangToWhen(): FirWhenExpression {
            return baseExpression.toFirExpression("No operand").generateNotNullOrOther(
                session,
                FirThrowExpressionImpl(
                    session, this, FirFunctionCallImpl(session, this).apply {
                        calleeReference = FirSimpleNamedReference(this@RawFirBuilder.session, this@bangBangToWhen, KNPE)
                    }
                ), "bangbang", this
            )
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val leftArgument = expression.left.toFirExpression("No left operand")
            val rightArgument = expression.right.toFirExpression("No right operand")
            when (operationToken) {
                ELVIS ->
                    return leftArgument.generateNotNullOrOther(session, rightArgument, "elvis", expression)
                ANDAND, OROR ->
                    return leftArgument.generateLazyLogicalOperation(session, rightArgument, operationToken == ANDAND, expression)
                in OperatorConventions.IN_OPERATIONS ->
                    return rightArgument.generateContainsOperation(
                        session, leftArgument, operationToken == NOT_IN, expression, expression.operationReference
                    )
            }
            val conventionCallName = operationToken.toBinaryName()
            return if (conventionCallName != null || operationToken == IDENTIFIER) {
                FirFunctionCallImpl(
                    session, expression
                ).apply {
                    calleeReference = FirSimpleNamedReference(
                        this@RawFirBuilder.session, expression.operationReference,
                        conventionCallName ?: expression.operationReference.getReferencedNameAsName()
                    )
                    explicitReceiver = leftArgument
                    arguments += rightArgument
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                if (firOperation in FirOperation.ASSIGNMENTS) {
                    return expression.left.generateAssignment(session, expression, rightArgument, firOperation) {
                        toFirExpression("Incorrect expression in assignment: ${expression.text}")
                    }
                } else {
                    FirOperatorCallImpl(session, expression, firOperation).apply {
                        arguments += leftArgument
                        arguments += rightArgument
                    }
                }
            }
        }

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Unit): FirElement {
            val operation = expression.operationReference.getReferencedNameElementType().toFirOperation()
            return FirTypeOperatorCallImpl(
                session, expression, operation, expression.right.toFirOrErrorType()
            ).apply {
                arguments += expression.left.toFirExpression("No left operand")
            }
        }

        override fun visitIsExpression(expression: KtIsExpression, data: Unit): FirElement {
            return FirTypeOperatorCallImpl(
                session, expression, if (expression.isNegated) FirOperation.NOT_IS else FirOperation.IS,
                expression.typeReference.toFirOrErrorType()
            ).apply {
                arguments += expression.leftHandSide.toFirExpression("No left operand")
            }
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): FirElement {
            val operationToken = expression.operationToken
            val argument = expression.baseExpression
            if (operationToken == EXCLEXCL) {
                return expression.bangBangToWhen()
            }
            val conventionCallName = operationToken.toUnaryName()
            return if (conventionCallName != null) {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        session, expression, argument,
                        callName = conventionCallName,
                        prefix = expression is KtPrefixExpression
                    ) { toFirExpression("Incorrect expression inside inc/dec") }
                }
                FirFunctionCallImpl(
                    session, expression
                ).apply {
                    calleeReference = FirSimpleNamedReference(
                        this@RawFirBuilder.session, expression.operationReference, conventionCallName
                    )
                    explicitReceiver = argument.toFirExpression("No operand")
                }
            } else {
                val firOperation = operationToken.toFirOperation()
                FirOperatorCallImpl(
                    session, expression, firOperation
                ).apply {
                    arguments += argument.toFirExpression("No operand")
                }
            }
        }

        private val firFunctionCalls = mutableListOf<FirFunctionCall>()

        override fun visitCallExpression(expression: KtCallExpression, data: Unit): FirElement {
            val calleeExpression = expression.calleeExpression
            return FirFunctionCallImpl(session, expression).apply {
                val calleeReference = when (calleeExpression) {
                    is KtSimpleNameExpression -> FirSimpleNamedReference(
                        this@RawFirBuilder.session, calleeExpression, calleeExpression.getReferencedNameAsName()
                    )
                    null -> FirErrorNamedReference(
                        this@RawFirBuilder.session, calleeExpression, "Call has no callee"
                    )
                    else -> {
                        arguments += calleeExpression.toFirExpression("Incorrect invoke receiver")
                        FirSimpleNamedReference(
                            this@RawFirBuilder.session, expression, OperatorNameConventions.INVOKE
                        )
                    }
                }
                this.calleeReference = calleeReference
                firFunctionCalls += this
                expression.extractArgumentsTo(this)
                for (typeArgument in expression.typeArguments) {
                    typeArguments += typeArgument.convert<FirTypeProjection>()
                }
                firFunctionCalls.removeLast()
            }
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Unit): FirElement {
            val arrayExpression = expression.arrayExpression
            return FirFunctionCallImpl(session, expression).apply {
                calleeReference = FirSimpleNamedReference(this@RawFirBuilder.session, expression, OperatorNameConventions.GET)
                explicitReceiver = arrayExpression.toFirExpression("No array expression")
                for (indexExpression in expression.indexExpressions) {
                    arguments += indexExpression.toFirExpression("Incorrect index expression")
                }
            }
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Unit): FirElement {
            val selector = expression.selectorExpression
                ?: return FirErrorExpressionImpl(session, expression, "Qualified expression without selector")
            val firSelector = selector.toFirExpression("Incorrect selector expression")
            if (firSelector is FirModifiableQualifiedAccess) {
                firSelector.safe = expression is KtSafeQualifiedExpression
                firSelector.explicitReceiver = expression.receiverExpression.toFirExpression("Incorrect receiver expression")
            }
            return firSelector
        }

        override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
            val labelName = expression.getLabelName()
            return FirQualifiedAccessExpressionImpl(session, expression).apply {
                calleeReference = FirExplicitThisReference(this@RawFirBuilder.session, expression, labelName)
            }
        }

        override fun visitSuperExpression(expression: KtSuperExpression, data: Unit): FirElement {
            val superType = expression.superTypeQualifier
            return FirQualifiedAccessExpressionImpl(session, expression).apply {
                calleeReference = FirExplicitSuperReference(this@RawFirBuilder.session, expression, superType.toFirOrImplicitType())
            }
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): FirElement {
            return expression.expression?.accept(this, data) ?: FirErrorExpressionImpl(session, expression, "Empty parentheses")
        }

        private val firLabels = mutableListOf<FirLabel>()

        override fun visitLabeledExpression(expression: KtLabeledExpression, data: Unit): FirElement {
            val labelName = expression.getLabelName()
            val size = firLabels.size
            if (labelName != null) {
                firLabels += FirLabelImpl(session, expression, labelName)
            }
            val result = expression.baseExpression?.accept(this, data) ?: FirErrorExpressionImpl(session, expression, "Empty label")
            if (size != firLabels.size) {
                firLabels.removeLast()
                println("Unused label: ${expression.text}")
            }
            return result
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Unit): FirElement {
            val rawResult = expression.baseExpression?.accept(this, data)
            val result = rawResult as? FirAbstractAnnotatedElement
                ?: FirErrorExpressionImpl(session, expression, "Strange annotated expression: ${rawResult?.render()}")
            expression.extractAnnotationsTo(result)
            return result
        }

        override fun visitThrowExpression(expression: KtThrowExpression, data: Unit): FirElement {
            return FirThrowExpressionImpl(session, expression, expression.thrownExpression.toFirExpression("Nothing to throw"))
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
            return FirGetClassCallImpl(session, expression).apply {
                arguments += expression.receiverExpression.toFirExpression("No receiver in class literal")
            }
        }

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Unit): FirElement {
            return FirCallableReferenceAccessImpl(session, expression).apply {
                calleeReference = FirSimpleNamedReference(
                    this@RawFirBuilder.session, expression.callableReference, expression.callableReference.getReferencedNameAsName()
                )
                explicitReceiver = expression.receiverExpression?.toFirExpression("Incorrect receiver expression")
            }
        }

        override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Unit): FirElement {
            return FirArrayOfCallImpl(session, expression).apply {
                for (innerExpression in expression.getInnerExpressions()) {
                    arguments += innerExpression.toFirExpression("Incorrect collection literal argument")
                }
            }
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return FirExpressionStub(session, expression)
        }
    }

    companion object {
        val KNPE = Name.identifier("KotlinNullPointerException")
    }
}