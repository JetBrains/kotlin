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

package org.jetbrains.kotlin.idea.completion

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.*

interface ContextVariablesProvider {
    fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>>
}

class RealContextVariablesProvider(
        private val referenceVariantsHelper: ReferenceVariantsHelper,
        private val contextElement: PsiElement
) : ContextVariablesProvider {

    val allFunctionTypeVariables by lazy {
        collectVariables().filter { it.type.isBuiltinFunctionalTypeOrSubtype }
    }

    private fun collectVariables(): Collection<VariableDescriptor> {
        val descriptorFilter = DescriptorKindFilter.VARIABLES exclude DescriptorKindExclude.Extensions // we exclude extensions by performance reasons
        return referenceVariantsHelper.getReferenceVariants(contextElement, CallTypeAndReceiver.DEFAULT, descriptorFilter, nameFilter = { true })
                .map { it as VariableDescriptor }
    }

    override fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>> {
        val result = SmartList<Pair<VariableDescriptor, TypeSubstitutor>>()
        for (variable in allFunctionTypeVariables) {
            val substitutor = variable.fuzzyReturnType()?.checkIsSubtypeOf(requiredType) ?: continue
            result.add(variable to substitutor)
        }
        return result
    }
}

class CollectRequiredTypesContextVariablesProvider : ContextVariablesProvider {
    private val _requiredTypes = HashSet<FuzzyType>()

    val requiredTypes: Set<FuzzyType>
        get() = _requiredTypes

    override fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>> {
        _requiredTypes.add(requiredType)
        return emptyList()
    }
}
