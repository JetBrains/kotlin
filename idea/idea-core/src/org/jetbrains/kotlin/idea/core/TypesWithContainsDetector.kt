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

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator
import java.util.*

internal class TypesWithContainsDetector(
        private val scope: LexicalScope,
        private val argumentType: KotlinType
) {
    private val cache = HashMap<FuzzyType, Boolean>()

    private val typesWithExtensionContains: Collection<KotlinType> = scope
            .collectFunctions(OperatorNameConventions.CONTAINS, NoLookupLocation.FROM_IDE)
            .filter { it.extensionReceiverParameter != null && isGoodContainsFunction(it, listOf()) }
            .map { it.extensionReceiverParameter!!.type }

    fun hasContains(type: FuzzyType): Boolean {
        return cache.getOrPut(type, { hasContainsNoCache(type) })
    }

    private fun hasContainsNoCache(type: FuzzyType): Boolean {
        return type.nullability() != TypeNullability.NULLABLE &&
               type.type.memberScope.getContributedFunctions(OperatorNameConventions.CONTAINS, NoLookupLocation.FROM_IDE).any { isGoodContainsFunction(it, type.freeParameters) }
               || typesWithExtensionContains.any { type.checkIsSubtypeOf(it) != null }
    }

    private fun isGoodContainsFunction(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        if (!function.isValidOperator()) return false
        val parameter = function.valueParameters.single()
        val fuzzyParameterType = FuzzyType(parameter.type, function.typeParameters + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType) != null
    }
}
