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

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.scopes.JetScope
import java.util.HashMap
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.idea.util.TypeNullability
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.HeuristicSignatures

class TypesWithContainsDetector(
        private val scope: JetScope,
        private val argumentType: JetType,
        private val project: Project,
        private val moduleDescriptor: ModuleDescriptor
) {

    private val cache = HashMap<FuzzyType, Boolean>()
    private val containsName = Name.identifier("contains")
    private val booleanType = KotlinBuiltIns.getInstance().getBooleanType()

    private val typesWithExtensionContains: Collection<JetType> = scope.getFunctions(containsName)
            .filter { it.getExtensionReceiverParameter() != null && isGoodContainsFunction(it, listOf()) }
            .map { it.getExtensionReceiverParameter()!!.getType() }

    public fun hasContains(type: FuzzyType): Boolean {
        return cache.getOrPut(type, { hasContainsNoCache(type) })
    }

    private fun hasContainsNoCache(type: FuzzyType): Boolean {
        return type.nullability() != TypeNullability.NULLABLE && type.type.getMemberScope().getFunctions(containsName).any { isGoodContainsFunction(it, type.freeParameters) }
               || typesWithExtensionContains.any { type.checkIsSubtypeOf(it) != null }
    }

    private fun isGoodContainsFunction(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        if (!TypeUtils.equalTypes(function.getReturnType(), booleanType)) return false
        val parameter = function.getValueParameters().singleOrNull() ?: return false
        val parameterType = HeuristicSignatures.correctedParameterType(function, 0, moduleDescriptor, project) ?: parameter.getType()
        val fuzzyParameterType = FuzzyType(parameterType, function.getTypeParameters() + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType) != null
    }
}
