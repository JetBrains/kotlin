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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator
import java.util.*

abstract class TypesWithOperatorDetector(
        private val name: Name,
        private val scope: LexicalScope,
        private val indicesHelper: KotlinIndicesHelper?
) {
    protected abstract fun isSuitableByType(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean

    private val cache = HashMap<FuzzyType, FunctionDescriptor?>()

    private val typesWithExtensionFromScope: Map<KotlinType, FunctionDescriptor> by lazy {
        scope.collectFunctions(name, NoLookupLocation.FROM_IDE)
                .filter { it.extensionReceiverParameter != null && it.isValidOperator() && isSuitableByType(it, it.typeParameters) }
                .map { it.extensionReceiverParameter!!.type to it }
                .toMap()
    }

    private val typesWithExtensionFromIndices: Map<KotlinType, FunctionDescriptor> by lazy {
        indicesHelper?.getTopLevelExtensionOperatorsByName(name.asString())
                ?.filter { it.extensionReceiverParameter != null && it.isValidOperator() && isSuitableByType(it, it.typeParameters) }
                ?.map { it.extensionReceiverParameter!!.type to it }
                ?.filter { it.first !in typesWithExtensionFromScope.keys }
                ?.toMap() ?: emptyMap()
    }

    fun findOperator(type: FuzzyType): FunctionDescriptor? {
        if (cache.containsKey(type)) {
            return cache[type]
        }
        else {
            val result = findOperatorNoCache(type)
            cache[type] = result
            return result
        }
    }

    private fun findOperatorNoCache(type: FuzzyType): FunctionDescriptor? {
        if (type.nullability() != TypeNullability.NULLABLE) {
            val memberFunction = type.type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                    .firstOrNull { it.isValidOperator() && isSuitableByType(it, type.freeParameters) }
            if (memberFunction != null) return memberFunction
        }

        for ((typeWithExtension, operator) in typesWithExtensionFromScope + typesWithExtensionFromIndices) {
            if (type.checkIsSubtypeOf(typeWithExtension) != null) {
                return operator //TODO: substitution
            }
        }

        return null
    }
}

class TypesWithContainsDetector(
        scope: LexicalScope,
        indicesHelper: KotlinIndicesHelper?,
        private val argumentType: KotlinType
) : TypesWithOperatorDetector(OperatorNameConventions.CONTAINS, scope, indicesHelper) {

    override fun isSuitableByType(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        val parameter = function.valueParameters.single()
        val fuzzyParameterType = FuzzyType(parameter.type, function.typeParameters + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType) != null
    }
}

class TypesWithGetValueDetector(
        scope: LexicalScope,
        indicesHelper: KotlinIndicesHelper?,
        private val propertyOwnerType: KotlinType,
        private val propertyType: KotlinType?
) : TypesWithOperatorDetector(OperatorNameConventions.GET_VALUE, scope, indicesHelper) {

    override fun isSuitableByType(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        val paramType = FuzzyType(function.valueParameters.first().type, freeTypeParams)
        if (paramType.checkIsSuperTypeOf(propertyOwnerType) == null) return false

        if (propertyType != null) {
            val returnType = FuzzyType(function.returnType ?: return false, freeTypeParams)
            return returnType.checkIsSubtypeOf(propertyType) != null
        }

        return true
    }
}

class TypesWithSetValueDetector(
        scope: LexicalScope,
        indicesHelper: KotlinIndicesHelper?,
        private val propertyOwnerType: KotlinType
) : TypesWithOperatorDetector(OperatorNameConventions.SET_VALUE, scope, indicesHelper) {

    override fun isSuitableByType(function: FunctionDescriptor, freeTypeParams: Collection<TypeParameterDescriptor>): Boolean {
        val paramType = FuzzyType(function.valueParameters.first().type, freeTypeParams)
        return paramType.checkIsSuperTypeOf(propertyOwnerType) != null
    }
}