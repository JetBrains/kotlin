/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance

class RawFirBuilder(val session: FirSession) {

    private val implicitUnitType = FirImplicitUnitType(session, null)

    private val implicitAnyType = FirImplicitAnyType(session, null)

    private val implicitEnumType = FirImplicitEnumType(session, null)

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

    private val KtDeclaration.platformStatus: FirMemberPlatformStatus
        get() {
            return when {
                hasExpectModifier() -> FirMemberPlatformStatus.EXPECT
                hasActualModifier() -> FirMemberPlatformStatus.ACTUAL
                else -> FirMemberPlatformStatus.DEFAULT
            }
        }

    private inner class Visitor : KtVisitor<FirElement, Unit>() {
        private inline fun <reified R : FirElement> KtElement?.convertSafe(): R? =
            this?.accept(this@Visitor, Unit) as? R

        private inline fun <reified R : FirElement> KtElement.convert(): R =
            this.accept(this@Visitor, Unit) as R

        private fun KtTypeReference?.toFirOrImplicitType(): FirType =
            convertSafe() ?: FirImplicitTypeImpl(session, this)

        private fun KtTypeReference?.toFirOrUnitType(): FirType =
            convertSafe() ?: implicitUnitType

        private fun KtTypeReference?.toFirOrErrorType(): FirType =
            convertSafe() ?: FirErrorTypeImpl(session, this, if (this == null) "Incomplete code" else "Conversion failed")

        private fun KtDeclarationWithBody.buildFirBody(): FirBody? =
            when {
                !hasBody() -> null
                hasBlockBody() -> FirBlockBodyImpl(session, this)
                else -> FirExpressionBodyImpl(session, FirExpressionStub(session, null))
            }

        private fun ValueArgument?.toFirExpression(): FirExpression = with(this as? KtElement) {
            convertSafe() ?: FirErrorExpressionImpl(session, this)
        }

        private fun KtPropertyAccessor?.toFirPropertyAccessor(
            property: KtProperty,
            propertyType: FirType,
            isGetter: Boolean
        ): FirPropertyAccessor {
            if (this == null) {
                return if (isGetter) {
                    FirDefaultPropertyGetter(session, property, propertyType)
                } else {
                    FirDefaultPropertySetter(session, property, propertyType)
                }
            }
            val firAccessor = FirPropertyAccessorImpl(
                session,
                this,
                isGetter,
                visibility,
                if (isGetter) {
                    returnTypeReference.toFirOrImplicitType()
                } else {
                    returnTypeReference.toFirOrUnitType()
                },
                this.buildFirBody()
            )
            extractAnnotationsTo(firAccessor)
            extractValueParametersTo(firAccessor, propertyType)
            if (!isGetter && firAccessor.valueParameters.isEmpty()) {
                firAccessor.valueParameters += FirDefaultSetterValueParameter(session, this, propertyType)
            }
            return firAccessor
        }

        private fun KtParameter.toFirValueParameter(defaultType: FirType? = null): FirValueParameter {
            val firValueParameter = FirValueParameterImpl(
                session,
                this,
                nameAsSafeName,
                when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultType != null -> defaultType
                    else -> null.toFirOrErrorType()
                },
                if (hasDefaultValue()) FirExpressionStub(session, this) else null,
                isCrossinline = hasModifier(KtTokens.CROSSINLINE_KEYWORD),
                isNoinline = hasModifier(KtTokens.NOINLINE_KEYWORD),
                isVararg = isVarArg
            )
            extractAnnotationsTo(firValueParameter)
            return firValueParameter
        }

        private fun KtParameter.toFirProperty(): FirProperty {
            require(hasValOrVar())
            val type = typeReference.toFirOrErrorType()
            val firProperty = FirMemberPropertyImpl(
                session,
                this,
                nameAsSafeName,
                visibility,
                modality,
                platformStatus,
                isOverride = hasModifier(KtTokens.OVERRIDE_KEYWORD),
                isConst = false,
                isLateInit = false,
                receiverType = null,
                returnType = type,
                isVar = isMutable,
                initializer = null,
                getter = FirDefaultPropertyGetter(session, this, type),
                setter = FirDefaultPropertySetter(session, this, type),
                delegate = null
            )
            extractAnnotationsTo(firProperty)
            return firProperty
        }

        private fun KtModifierListOwner.extractAnnotationsTo(container: FirAbstractAnnotatedDeclaration) {
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
            container: FirAbstractFunction,
            defaultType: FirType? = null
        ) {
            for (valueParameter in valueParameters) {
                container.valueParameters += valueParameter.toFirValueParameter(defaultType)
            }
        }

        private fun KtCallElement.extractArgumentsTo(container: FirAbstractCall) {
            for (argument in this.valueArguments) {
                container.arguments += argument.toFirExpression()
            }
        }

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(container: FirClassImpl) {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            var delegatedSuperType: FirType? = null
            for (superTypeListEntry in superTypeListEntries) {
                when (superTypeListEntry) {
                    is KtSuperTypeEntry -> {
                        container.superTypes += superTypeListEntry.typeReference.toFirOrErrorType()
                    }
                    is KtSuperTypeCallEntry -> {
                        delegatedSuperType = superTypeListEntry.calleeExpression.typeReference.toFirOrErrorType()
                        container.superTypes += delegatedSuperType
                        superTypeCallEntry = superTypeListEntry
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        val type = superTypeListEntry.typeReference.toFirOrErrorType()
                        container.superTypes += FirDelegatedTypeImpl(
                            type,
                            FirExpressionStub(session, superTypeListEntry)
                        )
                    }
                }
            }
            fun isEnum() = this is KtClass && this.isEnum()
            if (this is KtClass && this.isInterface()) return
            if (!this.hasPrimaryConstructor()) return
            val firPrimaryConstructor = primaryConstructor.toFirConstructor(
                superTypeCallEntry,
                delegatedSuperType = delegatedSuperType ?: (if (isEnum()) implicitEnumType else implicitAnyType),
                owner = this
            )
            container.declarations += firPrimaryConstructor
        }

        private fun KtPrimaryConstructor?.toFirConstructor(
            superTypeCallEntry: KtSuperTypeCallEntry?,
            delegatedSuperType: FirType,
            owner: KtClassOrObject
        ): FirConstructor {
            val constructorCallee = superTypeCallEntry?.calleeExpression
            val firDelegatedCall = FirDelegatedConstructorCallImpl(
                session,
                constructorCallee ?: (this ?: owner),
                delegatedSuperType,
                isThis = false
            ).apply {
                // TODO: arguments are not needed for light classes, but will be needed later
                //superTypeCallEntry.extractArgumentsTo(this)
            }
            val firConstructor = FirPrimaryConstructorImpl(
                session,
                this ?: owner,
                this?.visibility ?: Visibilities.UNKNOWN,
                firDelegatedCall
            )
            this?.extractAnnotationsTo(firConstructor)
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

        override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit): FirElement {
            return withChildClassName(enumEntry.nameAsSafeName) {
                val firEnumEntry = FirEnumEntryImpl(
                    session,
                    enumEntry,
                    FirClassSymbol(currentClassId),
                    enumEntry.nameAsSafeName
                )
                enumEntry.extractAnnotationsTo(firEnumEntry)
                enumEntry.extractSuperTypeListEntriesTo(firEnumEntry)
                for (declaration in enumEntry.declarations) {
                    firEnumEntry.declarations += declaration.convert<FirDeclaration>()
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
                    classOrObject.visibility,
                    classOrObject.modality,
                    classOrObject.platformStatus,
                    classKind,
                    isInner = classOrObject.hasModifier(KtTokens.INNER_KEYWORD),
                    isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true,
                    isData = (classOrObject as? KtClass)?.isData() == true,
                    isInline = classOrObject.hasModifier(KtTokens.INLINE_KEYWORD)
                )
                classOrObject.extractAnnotationsTo(firClass)
                classOrObject.extractTypeParametersTo(firClass)
                classOrObject.extractSuperTypeListEntriesTo(firClass)
                classOrObject.primaryConstructor?.valueParameters?.forEach {
                    if (it.hasValOrVar()) {
                        firClass.declarations += it.toFirProperty()
                    }
                }

                for (declaration in classOrObject.declarations) {
                    firClass.declarations += declaration.convert<FirDeclaration>()
                }

                firClass
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
                    typeAlias.platformStatus,
                    typeAlias.getTypeReference().toFirOrErrorType()
                )
                typeAlias.extractAnnotationsTo(firTypeAlias)
                typeAlias.extractTypeParametersTo(firTypeAlias)
                firTypeAlias
            }
        }

        override fun visitNamedFunction(function: KtNamedFunction, data: Unit): FirElement {
            if (function.name == null) {
                // TODO: return anonymous function here
                // TODO: what if name is not null but we're in expression position?
                return FirExpressionStub(session, function)
            }
            val typeReference = function.typeReference
            val firFunction = FirMemberFunctionImpl(
                session,
                function,
                function.nameAsSafeName,
                function.visibility,
                function.modality,
                function.platformStatus,
                function.hasModifier(KtTokens.OVERRIDE_KEYWORD),
                function.hasModifier(KtTokens.OPERATOR_KEYWORD),
                function.hasModifier(KtTokens.INFIX_KEYWORD),
                function.hasModifier(KtTokens.INLINE_KEYWORD),
                function.hasModifier(KtTokens.TAILREC_KEYWORD),
                function.hasModifier(KtTokens.EXTERNAL_KEYWORD),
                function.receiverTypeReference.convertSafe(),
                if (function.hasBlockBody()) {
                    typeReference.toFirOrUnitType()
                } else {
                    typeReference.toFirOrImplicitType()
                },
                function.buildFirBody()
            )
            function.extractAnnotationsTo(firFunction)
            function.extractTypeParametersTo(firFunction)
            for (valueParameter in function.valueParameters) {
                firFunction.valueParameters += valueParameter.convert<FirValueParameter>()
            }
            return firFunction
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit): FirElement {
            val firConstructor = FirConstructorImpl(
                session,
                constructor,
                constructor.visibility,
                constructor.getDelegationCall().convert(),
                constructor.buildFirBody()
            )
            constructor.extractAnnotationsTo(firConstructor)
            constructor.extractValueParametersTo(firConstructor)
            return firConstructor
        }

        override fun visitConstructorDelegationCall(call: KtConstructorDelegationCall, data: Unit): FirElement {
            val firConstructorCall = FirDelegatedConstructorCallImpl(
                session,
                call,
                FirErrorTypeImpl(session, call, "Not implemented yet"),
                call.isCallToThis || call.isImplicit
            )
            // TODO: arguments are not needed for light classes, but will be needed later
            // call.extractArgumentsTo(firConstructorCall)
            return firConstructorCall
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return FirAnonymousInitializerImpl(
                session,
                initializer,
                FirBlockBodyImpl(session, initializer)
            )
        }

        override fun visitProperty(property: KtProperty, data: Unit): FirElement {
            val propertyType = property.typeReference.toFirOrImplicitType()
            val firProperty = FirMemberPropertyImpl(
                session,
                property,
                property.nameAsSafeName,
                property.visibility,
                property.modality,
                property.platformStatus,
                property.hasModifier(KtTokens.OVERRIDE_KEYWORD),
                property.hasModifier(KtTokens.CONST_KEYWORD),
                property.hasModifier(KtTokens.LATEINIT_KEYWORD),
                property.receiverTypeReference.convertSafe(),
                propertyType,
                property.isVar,
                if (property.hasInitializer()) FirExpressionStub(session, property) else null,
                property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true),
                property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false),
                if (property.hasDelegate()) FirExpressionStub(session, property) else null
            )
            property.extractAnnotationsTo(firProperty)
            property.extractTypeParametersTo(firProperty)
            return firProperty
        }

        override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): FirElement {
            val typeElement = typeReference.typeElement
            val isNullable = typeElement is KtNullableType

            fun KtTypeElement?.unwrapNullable(): KtTypeElement? =
                if (this is KtNullableType) this.innerType.unwrapNullable() else this

            val unwrappedElement = typeElement.unwrapNullable()
            val firType = when (unwrappedElement) {
                is KtDynamicType -> FirDynamicTypeImpl(session, typeReference, isNullable)
                is KtUserType -> {
                    var referenceExpression = unwrappedElement.referenceExpression
                    if (referenceExpression != null) {
                        val userType = FirUserTypeImpl(
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
                        FirErrorTypeImpl(session, typeReference, "Incomplete user type")
                    }
                }
                is KtFunctionType -> {
                    val functionType = FirFunctionTypeImpl(
                        session,
                        typeReference,
                        isNullable,
                        unwrappedElement.receiverTypeReference.convertSafe(),
                        unwrappedElement.returnTypeReference.toFirOrImplicitType()
                    )
                    for (valueParameter in unwrappedElement.parameters) {
                        functionType.valueParameters += valueParameter.convert<FirValueParameter>()
                    }
                    functionType
                }
                null -> FirErrorTypeImpl(session, typeReference, "Unwrapped type is null")
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
            val firTypeParameter = FirTypeParameterImpl(
                session,
                parameter,
                FirTypeParameterSymbol(),
                parameter.nameAsSafeName,
                parameter.variance,
                parameter.hasModifier(KtTokens.REIFIED_KEYWORD)
            )
            parameter.extractAnnotationsTo(firTypeParameter)
            val extendsBound = parameter.extendsBound
            // TODO: handle where, here or (preferable) in parent
            if (extendsBound != null) {
                firTypeParameter.bounds += extendsBound.convert<FirType>()
            }
            return firTypeParameter
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit): FirElement {
            val projectionKind = typeProjection.projectionKind
            if (projectionKind == KtProjectionKind.STAR) {
                return FirStarProjectionImpl(session, typeProjection)
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
            return FirBlockBodyImpl(session, expression)
        }

        override fun visitExpression(expression: KtExpression, data: Unit): FirElement {
            return FirExpressionStub(session, expression)
        }
    }
}