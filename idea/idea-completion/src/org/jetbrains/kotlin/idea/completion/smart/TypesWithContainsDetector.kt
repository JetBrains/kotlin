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

package org.jetbrains.kotlin.idea.completion.smart

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.completion.HeuristicSignatures
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import java.util.*

class TypesWithContainsDetector(
        private val scope: LexicalScope,
        private val argumentType: KotlinType,
        private val resolutionFacade: ResolutionFacade
) {
    private val cache = HashMap<FuzzyType, Boolean>()
    private val containsName = Name.identifier("contains")
    private val booleanType = resolutionFacade.moduleDescriptor.builtIns.booleanType
    private val heuristicSignatures = resolutionFacade.ideService<HeuristicSignatures>()

    private val typesWithExtensionContains: Collection<KotlinType> = scope
            .collectFunctions(containsName, NoLookupLocation.FROM_IDE)
            .filter { it.getExtensionReceiverParameter() != null && isGoodContainsFunction(it, listOf()) }
            .map { it.getExtensionReceiverParameter()!!.getType() }

    public fun hasContains(type: FuzzyType): Boolean {
        return cache.getOrPut(type, { hasContainsNoCache(type) })
    }

    private fun hasContainsNoCache(type: FuzzyType): Boolean {
        return type.nullability() != TypeNullability.NULLABLE &&
               type.type.memberScope.getFunctions(containsName, NoLookupLocation.FROM_IDE).any { isGoodContainsFunction(it, type.freeParameters) }
               || typesWithExtensionContains.any { type.checkIsSubtypeOf(it) != null }
    }

    private fun isGoodContainsFunction(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        if (!TypeUtils.equalTypes(function.getReturnType()!!, booleanType)) return false
        val parameter = function.getValueParameters().singleOrNull() ?: return false
        val parameterType = heuristicSignatures.correctedParameterType(function, parameter) ?: parameter.getType()
        val fuzzyParameterType = FuzzyType(parameterType, function.getTypeParameters() + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType) != null
    }
}
