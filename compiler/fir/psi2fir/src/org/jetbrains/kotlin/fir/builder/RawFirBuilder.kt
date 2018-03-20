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
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifierType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.types.Variance

class RawFirBuilder(val session: FirSession) {

    fun buildFirFile(file: KtFile): FirFile {
        return file.accept(Visitor(), Unit) as FirFile
    }

    private val KtModifierListOwner.visibility: Visibility
        get() {
            val modifierType = visibilityModifierType()
            return when (modifierType) {
                KtTokens.PUBLIC_KEYWORD -> Visibilities.PUBLIC
                KtTokens.PROTECTED_KEYWORD -> Visibilities.PROTECTED
                KtTokens.INTERNAL_KEYWORD -> Visibilities.INTERNAL
                KtTokens.PRIVATE_KEYWORD -> Visibilities.PRIVATE
                else -> Visibilities.UNKNOWN
            }
        }

    private val KtDeclaration.modality: Modality
        get() {
            val modifierType = modalityModifierType()
            return when (modifierType) {
                KtTokens.FINAL_KEYWORD -> Modality.FINAL
                KtTokens.SEALED_KEYWORD -> Modality.SEALED
                KtTokens.ABSTRACT_KEYWORD -> Modality.ABSTRACT
                KtTokens.OPEN_KEYWORD -> Modality.OPEN
                else -> Modality.FINAL // FIX ME
            }
        }

    private inner class Visitor : KtVisitor<FirElement, Unit>() {
        @Suppress("UNCHECKED_CAST")
        private fun <R : FirElement> KtElement?.convertSafe(): R? =
            this?.accept(this@Visitor, Unit) as? R

        @Suppress("UNCHECKED_CAST")
        private fun <R : FirElement> KtElement.convert(): R =
            this.accept(this@Visitor, Unit) as R

        private fun KtTypeReference?.toFirOrImplicitType(): FirType =
            convertSafe() ?: FirImplicitTypeImpl(session, this)

        private fun KtTypeReference?.toFirOrErrorType(): FirType =
            convertSafe() ?: FirErrorTypeImpl(session, this, if (this == null) "Incomplete code" else "Conversion failed")

        private fun KtExpression?.toFirBody(): FirBody? =
            convertSafe<FirExpression>()?.let { it as? FirBody ?: FirExpressionBodyImpl(session, it) }

        private fun KtExpression?.toFirExpression(): FirExpression =
            convertSafe() ?: FirErrorExpressionImpl(session, this)

        private fun KtPropertyAccessor?.toFirPropertyAccessor(
            property: KtProperty,
            propertyType: FirType,
            isGetter: Boolean
        ): FirPropertyAccessor {
            if (this == null) {
                return FirDefaultPropertyAccessor(session, property, isGetter, propertyType)
            }
            val firAccessor = FirPropertyAccessorImpl(
                session,
                this,
                isGetter,
                visibility,
                returnTypeReference.toFirOrImplicitType(),
                bodyExpression.toFirBody()
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
                hasValOrVar(),
                nameAsSafeName,
                when {
                    typeReference != null -> typeReference.toFirOrErrorType()
                    defaultType != null -> defaultType
                    else -> null.toFirOrErrorType()
                },
                defaultValue?.convert(),
                isCrossinline = hasModifier(KtTokens.CROSSINLINE_KEYWORD),
                isNoinline = hasModifier(KtTokens.NOINLINE_KEYWORD),
                isVararg = isVarArg
            )
            extractAnnotationsTo(firValueParameter)
            return firValueParameter
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
                container.arguments += argument.getArgumentExpression().toFirExpression()
            }
        }

        private fun KtClassOrObject.extractSuperTypeListEntriesTo(container: FirClassImpl) {
            var superTypeCallEntry: KtSuperTypeCallEntry? = null
            for (superTypeListEntry in superTypeListEntries) {
                when (superTypeListEntry) {
                    is KtSuperTypeEntry -> {
                        container.superTypes += superTypeListEntry.typeReference.toFirOrErrorType()
                    }
                    is KtSuperTypeCallEntry -> {
                        container.superTypes += superTypeListEntry.calleeExpression.typeReference.toFirOrErrorType()
                        superTypeCallEntry = superTypeListEntry
                    }
                    is KtDelegatedSuperTypeEntry -> {
                        val type = superTypeListEntry.typeReference.toFirOrErrorType()
                        container.superTypes += FirDelegatedTypeImpl(
                            type,
                            superTypeListEntry.delegateExpression.convertSafe()
                        )
                    }
                }
            }
            val firPrimaryConstructor = primaryConstructor?.toFirConstructor(superTypeCallEntry) ?: return
            container.declarations += firPrimaryConstructor
        }

        private fun KtPrimaryConstructor.toFirConstructor(superTypeCallEntry: KtSuperTypeCallEntry?): FirConstructor {
            val constructorCallee = superTypeCallEntry?.calleeExpression
            val firDelegatedCall = constructorCallee?.let {
                FirDelegatedConstructorCallImpl(
                    session,
                    constructorCallee,
                    FirErrorTypeImpl(session, constructorCallee, "Not implemented yet"),
                    isThis = false
                ).apply {
                    superTypeCallEntry.extractArgumentsTo(this)
                }
            }
            val firConstructor = FirPrimaryConstructorImpl(
                session,
                this,
                visibility,
                firDelegatedCall
            )
            extractAnnotationsTo(firConstructor)
            extractValueParametersTo(firConstructor)
            return firConstructor
        }

        override fun visitKtFile(file: KtFile, data: Unit): FirElement {
            val firFile = FirFileImpl(session, file, file.name, file.packageFqName)
            for (annotationEntry in file.annotationEntries) {
                firFile.annotations += annotationEntry.convert<FirAnnotationCall>()
            }
            for (importDirective in file.importDirectives) {
                firFile.imports += FirImportImpl(
                    session,
                    importDirective,
                    importDirective.importedFqName,
                    importDirective.isAllUnder,
                    importDirective.aliasName
                )
            }
            for (declaration in file.declarations) {
                firFile.declarations += declaration.convert<FirDeclaration>()
            }
            return firFile
        }

        override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit): FirElement {
            val firEnumEntry = FirEnumEntryImpl(
                session,
                enumEntry,
                enumEntry.nameAsSafeName
            )
            enumEntry.extractAnnotationsTo(firEnumEntry)
            enumEntry.extractSuperTypeListEntriesTo(firEnumEntry)
            for (declaration in enumEntry.declarations) {
                firEnumEntry.declarations += declaration.convert<FirDeclaration>()
            }
            return firEnumEntry
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit): FirElement {
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
                classOrObject.nameAsSafeName,
                classOrObject.visibility,
                classOrObject.modality,
                classKind,
                isInner = classOrObject.hasModifier(KtTokens.INNER_KEYWORD),
                isCompanion = (classOrObject as? KtObjectDeclaration)?.isCompanion() == true,
                isData = (classOrObject as? KtClass)?.isData() == true
            )
            classOrObject.extractAnnotationsTo(firClass)
            classOrObject.extractTypeParametersTo(firClass)
            classOrObject.extractSuperTypeListEntriesTo(firClass)
            for (declaration in classOrObject.declarations) {
                firClass.declarations += declaration.convert<FirDeclaration>()
            }
            return firClass
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit): FirElement {
            val firTypeAlias = FirTypeAliasImpl(
                session,
                typeAlias,
                typeAlias.nameAsSafeName,
                typeAlias.visibility,
                typeAlias.getTypeReference().toFirOrErrorType()
            )
            typeAlias.extractAnnotationsTo(firTypeAlias)
            return firTypeAlias
        }

        override fun visitNamedFunction(function: KtNamedFunction, data: Unit): FirElement {
            if (function.name == null) {
                // TODO: return anonymous function here
                // TODO: what if name is not null but we're in expression position?
                return FirExpressionStub(session, function)
            }
            val firFunction = FirMemberFunctionImpl(
                session,
                function,
                function.nameAsSafeName,
                function.visibility,
                function.modality,
                function.hasModifier(KtTokens.OVERRIDE_KEYWORD),
                function.hasModifier(KtTokens.OPERATOR_KEYWORD),
                function.hasModifier(KtTokens.INFIX_KEYWORD),
                function.hasModifier(KtTokens.INLINE_KEYWORD),
                function.receiverTypeReference.convertSafe(),
                function.typeReference.toFirOrImplicitType(),
                function.bodyExpression.toFirBody()
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
                constructor.bodyExpression.toFirBody()
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
            call.extractArgumentsTo(firConstructorCall)
            return firConstructorCall
        }

        override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit): FirElement {
            return FirAnonymousInitializerImpl(
                session,
                initializer,
                initializer.body.toFirBody()
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
                property.hasModifier(KtTokens.OVERRIDE_KEYWORD),
                property.hasModifier(KtTokens.CONST_KEYWORD),
                property.receiverTypeReference.convertSafe(),
                propertyType,
                property.isVar,
                property.initializer?.convert(),
                property.getter.toFirPropertyAccessor(property, propertyType, isGetter = true),
                property.setter.toFirPropertyAccessor(property, propertyType, isGetter = false),
                property.delegateExpression?.convert()
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
                            userType.qualifier.add(0, firQualifier)

                            qualifier = qualifier.qualifier
                            referenceExpression = qualifier?.referenceExpression
                        } while (referenceExpression != null)
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
                parameter.nameAsSafeName,
                parameter.variance
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