/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val javaLangCloneable = FqNameUnsafe("java.lang.Cloneable")

object InlineClassDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        if (descriptor !is ClassDescriptor || !descriptor.isInline && !descriptor.isValue) return
        if (descriptor.kind != ClassKind.CLASS) return

        val trace = context.trace

        val valueKeyword = declaration.modifierList?.getModifier(KtTokens.VALUE_KEYWORD)

        // The check cannot be done in ModifierCheckerCore, since `value` keyword is enabled by one of two features, not by both of
        // them simultaneously
        if (valueKeyword != null) {
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JvmInlineValueClasses) &&
                !context.languageVersionSettings.supportsFeature(LanguageFeature.InlineClasses)
            ) {
                trace.report(
                    Errors.UNSUPPORTED_FEATURE.on(
                        valueKeyword, LanguageFeature.JvmInlineValueClasses to context.languageVersionSettings
                    )
                )
                return
            }
        }

        val inlineOrValueKeyword = declaration.modifierList?.getModifier(KtTokens.INLINE_KEYWORD) ?: valueKeyword
        require(inlineOrValueKeyword != null) { "Declaration of inline class must have 'inline' keyword" }

        if (descriptor.isInner || DescriptorUtils.isLocal(descriptor)) {
            trace.report(Errors.VALUE_CLASS_NOT_TOP_LEVEL.on(inlineOrValueKeyword))
            return
        }

        if (declaration.contextReceivers.isNotEmpty()) {
            val contextReceiverList = declaration.getContextReceiverList()
            requireNotNull(contextReceiverList) { "Declaration cannot have context receivers with no context receiver list" }
            trace.report(Errors.INLINE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS.on(contextReceiverList))
        }

        val modalityModifier = declaration.modalityModifier()
        if (modalityModifier != null && descriptor.modality != Modality.FINAL) {
            trace.report(Errors.VALUE_CLASS_NOT_FINAL.on(modalityModifier))
            return
        }

        val primaryConstructor = declaration.primaryConstructor
        if (primaryConstructor == null) {
            trace.report(Errors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS.on(inlineOrValueKeyword))
            return
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ValueClasses) && descriptor.isValueClass()) {
            if (primaryConstructor.valueParameters.isEmpty()) {
                (primaryConstructor.valueParameterList ?: declaration).let {
                    trace.report(Errors.VALUE_CLASS_EMPTY_CONSTRUCTOR.on(it))
                    return
                }
            }
        } else if (primaryConstructor.valueParameters.size != 1) {
            (primaryConstructor.valueParameterList ?: declaration).let {
                trace.report(Errors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE.on(it))
                return
            }
        }

        var baseParametersOk = true
        val baseParameterTypes = descriptor.safeAs<ClassDescriptor>()?.defaultType?.substitutedUnderlyingTypes() ?: emptyList()

        for ((baseParameter, baseParameterType) in primaryConstructor.valueParameters zip baseParameterTypes) {
            if (!isParameterAcceptableForInlineClass(baseParameter)) {
                trace.report(Errors.VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER.on(baseParameter))
                baseParametersOk = false
                continue
            }

            val baseParameterTypeReference = baseParameter.typeReference
            if (baseParameterType != null && baseParameterTypeReference != null) {
                if (baseParameterType.isInapplicableParameterType() &&
                    !(context.languageVersionSettings.supportsFeature(LanguageFeature.GenericInlineClassParameter) &&
                        (baseParameterType.isTypeParameter() || baseParameterType.isGenericArrayOfTypeParameter()))
                ) {
                    trace.report(Errors.VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE.on(baseParameterTypeReference, baseParameterType))
                    baseParametersOk = false
                    continue
                }

                if (baseParameterType.isRecursiveInlineOrValueClassType()) {
                    trace.report(Errors.VALUE_CLASS_CANNOT_BE_RECURSIVE.on(baseParameterTypeReference))
                    baseParametersOk = false
                    continue
                }
            }
        }
        if (!baseParametersOk) {
            return
        }

        for (supertypeEntry in declaration.superTypeListEntries) {
            val typeReference = supertypeEntry.typeReference ?: continue
            val type = trace[BindingContext.TYPE, typeReference] ?: continue
            if (supertypeEntry is KtDelegatedSuperTypeEntry) {
                val resolvedCall = supertypeEntry.delegateExpression.getResolvedCall(trace.bindingContext) ?: continue
                if (!context.languageVersionSettings.supportsFeature(LanguageFeature.InlineClassImplementationByDelegation) ||
                    resolvedCall.resultingDescriptor !is ValueParameterDescriptor ||
                    resolvedCall.resultingDescriptor.containingDeclaration != trace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, primaryConstructor]
                ) {
                    trace.report(Errors.VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION.on(supertypeEntry))
                    return
                }
            } else {
                val typeDescriptor = type.constructor.declarationDescriptor ?: continue
                if (!DescriptorUtils.isInterface(typeDescriptor)) {
                    trace.report(Errors.VALUE_CLASS_CANNOT_EXTEND_CLASSES.on(typeReference))
                    return
                }
            }
        }

        if (descriptor.getAllSuperClassifiers().any {
                it.fqNameUnsafe == StandardNames.FqNames.cloneable || it.fqNameUnsafe == javaLangCloneable
            }
        ) {
            trace.report(Errors.VALUE_CLASS_CANNOT_BE_CLONEABLE.on(inlineOrValueKeyword))
            return
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
        return parameter.hasValOrVar() && !parameter.isMutable && !parameter.isVarArg && !isOpen
    }
}

class PropertiesWithBackingFieldsInsideInlineClass : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtProperty) return
        if (descriptor !is PropertyDescriptor) return

        if (!descriptor.containingDeclaration.isInlineOrValueClass()) return

        if (context.trace.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) == true) {
            context.trace.report(Errors.PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS.on(declaration))
        }

        declaration.delegate?.let {
            context.trace.report(Errors.DELEGATED_PROPERTY_INSIDE_VALUE_CLASS.on(it))
        }
    }
}

class InnerClassInsideInlineClass : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtClass) return
        if (descriptor !is ClassDescriptor) return
        if (!descriptor.isInner) return

        if (!descriptor.containingDeclaration.isInlineOrValueClass()) return

        context.trace.report(Errors.INNER_CLASS_INSIDE_VALUE_CLASS.on(declaration.modifierList!!.getModifier(KtTokens.INNER_KEYWORD)!!))
    }
}

class ReservedMembersAndConstructsForInlineClass : DeclarationChecker {

    companion object {
        private val reservedFunctions = setOf("box", "unbox", "equals", "hashCode")
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val containingDeclaration = descriptor.containingDeclaration ?: return
        if (!containingDeclaration.isInlineOrValueClass()) return

        if (descriptor !is FunctionDescriptor) return

        when (descriptor) {
            is SimpleFunctionDescriptor -> {
                val ktFunction = declaration as? KtFunction ?: return
                val functionName = descriptor.name.asString()
                if (functionName in reservedFunctions) {
                    val nameIdentifier = ktFunction.nameIdentifier ?: return
                    context.trace.report(Errors.RESERVED_MEMBER_INSIDE_VALUE_CLASS.on(nameIdentifier, functionName))
                }
            }

            is ConstructorDescriptor -> {
                val secondaryConstructor = declaration as? KtSecondaryConstructor ?: return
                val bodyExpression = secondaryConstructor.bodyExpression
                if (secondaryConstructor.hasBlockBody() && bodyExpression is KtBlockExpression) {
                    val lBrace = bodyExpression.lBrace ?: return
                    context.trace.report(Errors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS.on(lBrace))
                }
            }
        }
    }
}
