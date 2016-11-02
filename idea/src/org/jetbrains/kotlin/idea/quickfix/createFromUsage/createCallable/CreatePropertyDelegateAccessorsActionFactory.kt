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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreatePropertyDelegateAccessorsActionFactory : CreateCallableMemberFromUsageFactory<KtExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtExpression? {
        return diagnostic.psiElement as? KtExpression
    }

    override fun extractFixData(element: KtExpression, diagnostic: Diagnostic): List<CallableInfo> {
        val context = element.analyze()

        fun isApplicableForAccessor(accessor: PropertyAccessorDescriptor?): Boolean =
                accessor != null && context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor] == null

        val property = element.getNonStrictParentOfType<KtProperty>() ?: return emptyList()
        val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor
                                 ?: return emptyList()

        val propertyReceiver = propertyDescriptor.extensionReceiverParameter ?: propertyDescriptor.dispatchReceiverParameter
        val propertyType = propertyDescriptor.type

        val accessorReceiverType = TypeInfo(element, Variance.IN_VARIANCE)
        val builtIns = propertyDescriptor.builtIns
        val thisRefParam = ParameterInfo(TypeInfo(propertyReceiver?.type ?: builtIns.nullableNothingType, Variance.IN_VARIANCE))
        val kPropertyStarType = ReflectionTypes.createKPropertyStarType(propertyDescriptor.module) ?: return emptyList()
        val metadataParam = ParameterInfo(TypeInfo(kPropertyStarType, Variance.IN_VARIANCE), "property")

        val callableInfos = SmartList<CallableInfo>()

        if (isApplicableForAccessor(propertyDescriptor.getter)) {
            val getterInfo = FunctionInfo(
                    name = OperatorNameConventions.GET_VALUE.asString(),
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(propertyType, Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam),
                    isOperator = true
            )
            callableInfos.add(getterInfo)
        }

        if (propertyDescriptor.isVar && isApplicableForAccessor(propertyDescriptor.setter)) {
            val newValueParam = ParameterInfo(TypeInfo(propertyType, Variance.IN_VARIANCE))
            val setterInfo = FunctionInfo(
                    name = OperatorNameConventions.SET_VALUE.asString(),
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(builtIns.unitType, Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam, newValueParam),
                    isOperator = true
            )
            callableInfos.add(setterInfo)
        }

        return callableInfos
    }
}
