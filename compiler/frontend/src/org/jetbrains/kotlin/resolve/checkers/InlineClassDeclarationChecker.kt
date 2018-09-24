/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object InlineClassDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        if (descriptor !is ClassDescriptor || !descriptor.isInline) return
        if (descriptor.kind != ClassKind.CLASS) return

        val inlineKeyword = declaration.modifierList?.getModifier(KtTokens.INLINE_KEYWORD)
        require(inlineKeyword != null) { "Declaration of inline class must have 'inline' keyword" }

        val trace = context.trace
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            trace.report(Errors.INLINE_CLASS_NOT_TOP_LEVEL.on(inlineKeyword))
            return
        }

        val modalityModifier = declaration.modalityModifier()
        if (modalityModifier != null && descriptor.modality != Modality.FINAL) {
            trace.report(Errors.INLINE_CLASS_NOT_FINAL.on(modalityModifier))
            return
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null) {
            trace.report(Errors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS.on(inlineKeyword))
            return
        }

        val primaryConstructorVisibility = descriptor.unsubstitutedPrimaryConstructor?.visibility
        if (primaryConstructorVisibility != null && primaryConstructorVisibility != Visibilities.PUBLIC) {
            trace.report(Errors.NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS.on(primaryConstructor.visibilityModifier() ?: inlineKeyword))
            return
        }

        val baseParameter = primaryConstructor.valueParameters.singleOrNull()
        if (baseParameter == null) {
            (primaryConstructor.valueParameterList ?: declaration).let {
                trace.report(Errors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE.on(it))
                return
            }
        }

        if (!isParameterAcceptableForInlineClass(baseParameter)) {
            trace.report(Errors.INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER.on(baseParameter))
            return
        }

        val anonymousInitializers = declaration.getAnonymousInitializers()
        if (anonymousInitializers.isNotEmpty()) {
            for (anonymousInitializer in anonymousInitializers) {
                if (anonymousInitializer is KtClassInitializer) {
                    trace.report(Errors.INLINE_CLASS_WITH_INITIALIZER.on(anonymousInitializer.initKeyword))
                }
            }

            return
        }

        val baseParameterType = descriptor.safeAs<ClassDescriptor>()?.defaultType?.substitutedUnderlyingType()
        val baseParameterTypeReference = baseParameter.typeReference
        if (baseParameterType != null && baseParameterTypeReference != null) {
            if (baseParameterType.isInapplicableParameterType()) {
                trace.report(Errors.INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE.on(baseParameterTypeReference, baseParameterType))
                return
            }

            if (baseParameterType.isRecursiveInlineClassType()) {
                trace.report(Errors.INLINE_CLASS_CANNOT_BE_RECURSIVE.on(baseParameterTypeReference))
                return
            }
        }

        for (supertypeEntry in declaration.superTypeListEntries) {
            if (supertypeEntry is KtDelegatedSuperTypeEntry) {
                trace.report(Errors.INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION.on(supertypeEntry))
                return
            } else {
                val typeReference = supertypeEntry.typeReference ?: continue
                val type = trace[BindingContext.TYPE, typeReference] ?: continue
                val typeDescriptor = type.constructor.declarationDescriptor ?: continue
                if (!DescriptorUtils.isInterface(typeDescriptor)) {
                    trace.report(Errors.INLINE_CLASS_CANNOT_EXTEND_CLASSES.on(typeReference))
                    return
                }
            }
        }
    }

    private fun KotlinType.isInapplicableParameterType() =
        isUnit() || isNothing() || isTypeParameter() || isGenericArrayOfTypeParameter()

    private fun KotlinType.isGenericArrayOfTypeParameter(): Boolean {
        if (!KotlinBuiltIns.isArray(this)) return false
        val argument0 = arguments[0]
        if (argument0.isStarProjection) return false
        val argument0type = argument0.type
        return argument0type.isTypeParameter() ||
                argument0type.isGenericArrayOfTypeParameter()
    }

    private fun isParameterAcceptableForInlineClass(parameter: KtParameter): Boolean {
        val isOpen = parameter.modalityModifier()?.node?.elementType == KtTokens.OPEN_KEYWORD
        return parameter.hasValOrVar() && !parameter.isMutable && !parameter.isVarArg && !parameter.hasDefaultValue() && !isOpen
    }
}

class PropertiesWithBackingFieldsInsideInlineClass : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtProperty) return
        if (descriptor !is PropertyDescriptor) return

        if (!descriptor.containingDeclaration.isInlineClass()) return

        if (context.trace.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) == true) {
            context.trace.report(Errors.PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS.on(declaration))
        }

        declaration.delegate?.let {
            context.trace.report(Errors.DELEGATED_PROPERTY_INSIDE_INLINE_CLASS.on(it))
        }
    }
}

class ReservedMembersAndConstructsForInlineClass : DeclarationChecker {

    companion object {
        private val reservedFunctions = setOf("box", "unbox", "equals", "hashCode")
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val containingDeclaration = descriptor.containingDeclaration ?: return
        if (!containingDeclaration.isInlineClass()) return

        if (descriptor !is FunctionDescriptor) return

        when (descriptor) {
            is SimpleFunctionDescriptor -> {
                val ktFunction = declaration as? KtFunction ?: return
                val functionName = descriptor.name.asString()
                if (functionName in reservedFunctions) {
                    val nameIdentifier = ktFunction.nameIdentifier ?: return
                    context.trace.report(Errors.RESERVED_MEMBER_INSIDE_INLINE_CLASS.on(nameIdentifier, functionName))
                }
            }

            is ConstructorDescriptor -> {
                val secondaryConstructor = declaration as? KtSecondaryConstructor ?: return
                val bodyExpression = secondaryConstructor.bodyExpression
                if (secondaryConstructor.hasBlockBody() && bodyExpression is KtBlockExpression) {
                    val lBrace = bodyExpression.lBrace ?: return
                    context.trace.report(Errors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS.on(lBrace))
                }
            }
        }
    }
}