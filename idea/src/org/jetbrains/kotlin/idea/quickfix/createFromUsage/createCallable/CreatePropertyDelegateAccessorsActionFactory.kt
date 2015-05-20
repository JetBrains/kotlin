/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory

object CreatePropertyDelegateAccessorsActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val expression = diagnostic.getPsiElement() as? JetExpression ?: return null
        val context = expression.analyze()

        fun isApplicableForAccessor(accessor: PropertyAccessorDescriptor?): Boolean =
                accessor != null && context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor] == null

        val builtIns = KotlinBuiltIns.getInstance()

        val property = expression.getNonStrictParentOfType<JetProperty>() ?: return null
        val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor
                                 ?: return null

        val propertyReceiver = propertyDescriptor.getExtensionReceiverParameter() ?: propertyDescriptor.getDispatchReceiverParameter()
        val propertyType = propertyDescriptor.getType()

        val accessorReceiverType = TypeInfo(expression, Variance.IN_VARIANCE)
        val thisRefParam = ParameterInfo(TypeInfo(propertyReceiver?.getType() ?: builtIns.getNullableNothingType(), Variance.IN_VARIANCE))
        val metadataParam = ParameterInfo(TypeInfo(builtIns.getPropertyMetadata().getDefaultType(), Variance.IN_VARIANCE))

        val callableInfos = SmartList<CallableInfo>()

        if (isApplicableForAccessor(propertyDescriptor.getGetter())) {
            val getterInfo = FunctionInfo(
                    name = "get",
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(propertyType, Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam)
            )
            callableInfos.add(getterInfo)
        }

        if (propertyDescriptor.isVar() && isApplicableForAccessor(propertyDescriptor.getSetter())) {
            val newValueParam = ParameterInfo(TypeInfo(propertyType, Variance.IN_VARIANCE))
            val setterInfo = FunctionInfo(
                    name = "set",
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(builtIns.getUnitType(), Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam, newValueParam)
            )
            callableInfos.add(setterInfo)
        }

        return CreateCallableFromUsageFixes(expression, callableInfos)
    }
}
