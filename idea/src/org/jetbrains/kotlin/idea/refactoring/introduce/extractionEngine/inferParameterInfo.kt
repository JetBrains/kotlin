/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.SingleType
import org.jetbrains.kotlin.cfg.pseudocode.getElementValuesRecursively
import org.jetbrains.kotlin.cfg.pseudocode.getExpectedTypePredicate
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithReceivers
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.hasBothReceivers
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.calls.util.createFunctionType
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.lang.AssertionError
import java.util.*

internal class ParametersInfo {
    var errorMessage: AnalysisResult.ErrorMessage? = null
    val originalRefToParameter = MultiMap.create<KtSimpleNameExpression, MutableParameter>()
    val parameters = HashSet<MutableParameter>()
    val typeParameters = HashSet<TypeParameter>()
    val nonDenotableTypes = HashSet<KotlinType>()
    val replacementMap = MultiMap.create<KtSimpleNameExpression, Replacement>()
}

internal fun ExtractionData.inferParametersInfo(
        commonParent: PsiElement,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        targetScope: LexicalScope,
        modifiedVarDescriptors: Set<VariableDescriptor>
): ParametersInfo {
    val info = ParametersInfo()

    val extractedDescriptorToParameter = HashMap<DeclarationDescriptor, MutableParameter>()

    for (refInfo in getBrokenReferencesInfo(createTemporaryCodeBlock())) {
        val ref = refInfo.refExpr

        val selector = (ref.parent as? KtCallExpression) ?: ref
        val superExpr = (selector.parent as? KtQualifiedExpression)?.receiverExpression as? KtSuperExpression
        if (superExpr != null) {
            info.errorMessage = AnalysisResult.ErrorMessage.SUPER_CALL
            return info
        }

        val resolvedCall = refInfo.resolveResult.resolvedCall
        val extensionReceiver = resolvedCall?.extensionReceiver
        val receiverToExtract = (if (extensionReceiver == null || isSynthesizedInvoke(refInfo.resolveResult.descriptor)) {
            resolvedCall?.dispatchReceiver
        }
        else {
            extensionReceiver
        }) as? ReceiverValue

        val twoReceivers = resolvedCall != null && resolvedCall.hasBothReceivers()
        val dispatchReceiverDescriptor = (resolvedCall?.dispatchReceiver as? ImplicitReceiver)?.declarationDescriptor
        if (twoReceivers
            && resolvedCall!!.extensionReceiver is ExpressionReceiver
            && DescriptorUtils.isCompanionObject(dispatchReceiverDescriptor)) {
            info.replacementMap.putValue(refInfo.resolveResult.originalRefExpr,
                                         WrapCompanionInWithReplacement(dispatchReceiverDescriptor as ClassDescriptor))
            continue
        }

        if (!refInfo.shouldSkipPrimaryReceiver) {
            extractReceiver(receiverToExtract, info, targetScope, refInfo, extractedDescriptorToParameter, pseudocode, bindingContext, false)
        }

        if (options.canWrapInWith && twoReceivers) {
            extractReceiver(resolvedCall!!.dispatchReceiver, info, targetScope, refInfo, extractedDescriptorToParameter, pseudocode, bindingContext, true)
        }
    }

    val varNameValidator = NewDeclarationNameValidator(
            commonParent.getNonStrictParentOfType<KtExpression>()!!,
            physicalElements.firstOrNull(),
            NewDeclarationNameValidator.Target.VARIABLES
    )

    for ((descriptorToExtract, parameter) in extractedDescriptorToParameter) {
        if (!parameter
                .getParameterType(options.allowSpecialClassNames)
                .processTypeIfExtractable(info.typeParameters, info.nonDenotableTypes, options, targetScope)) continue

        with (parameter) {
            if (currentName == null) {
                currentName = KotlinNameSuggester.suggestNamesByType(getParameterType(options.allowSpecialClassNames), varNameValidator, "p").first()
            }
            mirrorVarName = if (descriptorToExtract in modifiedVarDescriptors) KotlinNameSuggester.suggestNameByName(name, varNameValidator) else null
            info.parameters.add(this)
        }
    }

    for (typeToCheck in info.typeParameters.flatMapTo(HashSet<KotlinType>()) { it.collectReferencedTypes(bindingContext) }) {
        typeToCheck.processTypeIfExtractable(info.typeParameters, info.nonDenotableTypes, options, targetScope)
    }


    return info
}

private fun ExtractionData.extractReceiver(
        receiverToExtract: ReceiverValue?,
        info: ParametersInfo,
        targetScope: LexicalScope,
        refInfo: ResolvedReferenceInfo,
        extractedDescriptorToParameter: HashMap<DeclarationDescriptor, MutableParameter>,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        isMemberExtension: Boolean
) {
    val (originalRef, originalDeclaration, originalDescriptor, resolvedCall) = refInfo.resolveResult

    val thisDescriptor = (receiverToExtract as? ImplicitReceiver)?.declarationDescriptor
    val hasThisReceiver = thisDescriptor != null
    val thisExpr = refInfo.refExpr.parent as? KtThisExpression

    if (hasThisReceiver
        && DescriptorToSourceUtilsIde.getAllDeclarations(project, thisDescriptor!!).all { it.isInsideOf(physicalElements) }) {
        return
    }

    val referencedClassifierDescriptor: ClassifierDescriptor? = (thisDescriptor ?: originalDescriptor).let {
        when (it) {
            is ClassDescriptor ->
                when(it.kind) {
                    ClassKind.OBJECT, ClassKind.ENUM_CLASS -> it
                    ClassKind.ENUM_ENTRY -> it.containingDeclaration as? ClassDescriptor
                    else -> if (refInfo.refExpr.getNonStrictParentOfType<KtTypeReference>() != null) it else null
                }

            is TypeParameterDescriptor -> it

            is ConstructorDescriptor -> it.containingDeclaration

            else -> null
        } as? ClassifierDescriptor
    }

    if (referencedClassifierDescriptor != null) {
        if (!referencedClassifierDescriptor.defaultType.processTypeIfExtractable(
                info.typeParameters, info.nonDenotableTypes, options, targetScope, referencedClassifierDescriptor is TypeParameterDescriptor
        )) return

        if (options.canWrapInWith
            && resolvedCall != null
            && resolvedCall.hasBothReceivers()
            && DescriptorUtils.isCompanionObject(referencedClassifierDescriptor)) {
            info.replacementMap.putValue(originalRef, WrapCompanionInWithReplacement(referencedClassifierDescriptor as ClassDescriptor))
        } else if (referencedClassifierDescriptor is ClassDescriptor) {
            info.replacementMap.putValue(originalRef, FqNameReplacement(originalDescriptor.getImportableDescriptor().fqNameSafe))
        }
    }
    else {
        val extractThis = (hasThisReceiver && refInfo.smartCast == null) || thisExpr != null
        val extractOrdinaryParameter =
                originalDeclaration is KtDestructuringDeclarationEntry ||
                originalDeclaration is KtProperty ||
                originalDeclaration is KtParameter

        val extractFunctionRef =
                options.captureLocalFunctions
                && originalRef.getReferencedName() == originalDescriptor.name.asString() // to forbid calls by convention
                && originalDeclaration is KtNamedFunction && originalDeclaration.isLocal
                && targetScope.findFunction(originalDescriptor.name, NoLookupLocation.FROM_IDE) { it == originalDescriptor } == null

        val descriptorToExtract = (if (extractThis) thisDescriptor else null) ?: originalDescriptor

        val extractParameter = extractThis || extractOrdinaryParameter || extractFunctionRef
        if (extractParameter) {
            val parameterExpression = when {
                receiverToExtract is ExpressionReceiver -> {
                    val receiverExpression = receiverToExtract.expression
                    // If p.q has a smart-cast, then extract entire qualified expression
                    if (refInfo.smartCast != null) receiverExpression.parent as KtExpression else receiverExpression
                }
                receiverToExtract != null && refInfo.smartCast == null -> null
                else -> (originalRef.parent as? KtThisExpression) ?: originalRef
            }

            val parameterType = suggestParameterType(extractFunctionRef, originalDescriptor, parameterExpression, receiverToExtract, resolvedCall, true, bindingContext)

            val parameter = extractedDescriptorToParameter.getOrPut(descriptorToExtract) {
                var argumentText =
                        if (hasThisReceiver && extractThis) {
                            val label = if (descriptorToExtract is ClassDescriptor) "@${descriptorToExtract.name.asString()}" else ""
                            "this$label"
                        }
                        else {
                            val argumentExpr = (thisExpr ?: refInfo.refExpr).getQualifiedExpressionForSelectorOrThis()
                            if (argumentExpr is KtOperationReferenceExpression) {
                                val nameElement = argumentExpr.getReferencedNameElement()
                                val nameElementType = nameElement.node.elementType
                                (nameElementType as? KtToken)?.let {
                                    OperatorConventions.getNameForOperationSymbol(it)?.asString()
                                } ?: nameElement.text
                            }
                            else argumentExpr.text
                                 ?: throw AssertionError("reference shouldn't be empty: code fragment = $codeFragmentText")
                        }
                if (extractFunctionRef) {
                    val receiverTypeText = (originalDeclaration as KtCallableDeclaration).receiverTypeReference?.text ?: ""
                    argumentText = "$receiverTypeText::$argumentText"
                }

                val originalType = suggestParameterType(extractFunctionRef, originalDescriptor, parameterExpression, receiverToExtract, resolvedCall, false, bindingContext)

                MutableParameter(argumentText, descriptorToExtract, extractThis, targetScope, originalType, refInfo.possibleTypes)
            }

            if (!extractThis) {
                parameter.currentName = originalDeclaration.nameIdentifier?.text
            }

            parameter.refCount++
            info.originalRefToParameter.putValue(originalRef, parameter)

            parameter.addDefaultType(parameterType)

            if (extractThis && thisExpr == null) {
                val callElement = resolvedCall!!.call.callElement
                val instruction = pseudocode.getElementValue(callElement)?.createdAt as? InstructionWithReceivers
                val receiverValue = instruction?.receiverValues?.entries?.singleOrNull { it.value == receiverToExtract }?.key
                if (receiverValue != null) {
                    parameter.addTypePredicate(getExpectedTypePredicate(receiverValue, bindingContext, targetScope.ownerDescriptor.builtIns))
                }
            }
            else if (extractFunctionRef) {
                parameter.addTypePredicate(SingleType(parameterType))
            }
            else {
                pseudocode.getElementValuesRecursively(originalRef).forEach {
                    parameter.addTypePredicate(getExpectedTypePredicate(it, bindingContext, targetScope.ownerDescriptor.builtIns))
                }
            }

            val replacement = when {
                isMemberExtension -> WrapParameterInWithReplacement(parameter)
                hasThisReceiver && extractThis -> AddPrefixReplacement(parameter)
                else -> RenameReplacement(parameter)
            }
            info.replacementMap.putValue(originalRef, replacement)
        }
    }
}

private fun suggestParameterType(
        extractFunctionRef: Boolean,
        originalDescriptor: DeclarationDescriptor,
        parameterExpression: KtExpression?,
        receiverToExtract: ReceiverValue?,
        resolvedCall: ResolvedCall<*>?,
        useSmartCastsIfPossible: Boolean, bindingContext: BindingContext
): KotlinType {
    val builtIns = originalDescriptor.builtIns
    return when {
               extractFunctionRef -> {
                   originalDescriptor as FunctionDescriptor
                   createFunctionType(
                           builtIns,
                           Annotations.EMPTY,
                           originalDescriptor.extensionReceiverParameter?.type,
                           originalDescriptor.valueParameters.map { it.type },
                           originalDescriptor.valueParameters.map { it.name },
                           originalDescriptor.returnType ?: builtIns.defaultReturnType
                   )
               }

               parameterExpression != null ->
                   (if (useSmartCastsIfPossible) bindingContext[BindingContext.SMARTCAST, parameterExpression]?.defaultType else null)
                   ?: bindingContext.getType(parameterExpression)
                   ?: (parameterExpression as? KtReferenceExpression)?.let {
                       (bindingContext[BindingContext.REFERENCE_TARGET, it] as? CallableDescriptor)?.returnType
                   }
                   ?: receiverToExtract?.type

               receiverToExtract is ImplicitReceiver -> {
                   val typeByDataFlowInfo = if (useSmartCastsIfPossible) {
                       val dataFlowInfo = bindingContext.getDataFlowInfo(resolvedCall!!.call.callElement)
                       val possibleTypes = dataFlowInfo.getCollectedTypes(DataFlowValueFactory.createDataFlowValueForStableReceiver(receiverToExtract))
                       if (possibleTypes.isNotEmpty()) CommonSupertypes.commonSupertype(possibleTypes) else null
                   } else null
                   typeByDataFlowInfo ?: receiverToExtract.type
               }

               else -> receiverToExtract?.type
           } ?: builtIns.defaultParameterType
}
