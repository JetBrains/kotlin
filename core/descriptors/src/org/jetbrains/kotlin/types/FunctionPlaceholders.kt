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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class FunctionPlaceholders(private val builtIns: KotlinBuiltIns) {
    fun createFunctionPlaceholderType(
            argumentTypes: List<KotlinType>,
            hasDeclaredArguments: Boolean
    ): KotlinType {
        return ErrorUtils.createErrorTypeWithCustomConstructor(
                "function placeholder type",
                FunctionPlaceholderTypeConstructor(argumentTypes, hasDeclaredArguments, builtIns)
        )
    }
}

val KotlinType?.isFunctionPlaceholder: Boolean
    get() {
        return this != null && constructor is FunctionPlaceholderTypeConstructor
    }

class FunctionPlaceholderTypeConstructor(
        val argumentTypes: List<KotlinType>,
        val hasDeclaredArguments: Boolean,
        private val kotlinBuiltIns: KotlinBuiltIns
) : TypeConstructor {
    private val errorTypeConstructor: TypeConstructor = ErrorUtils.createErrorTypeConstructorWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE" + argumentTypes)

    override fun getParameters(): List<TypeParameterDescriptor> {
        return errorTypeConstructor.parameters
    }

    override fun getSupertypes(): Collection<KotlinType> {
        return errorTypeConstructor.supertypes
    }

    override fun isFinal(): Boolean {
        return errorTypeConstructor.isFinal
    }

    override fun isDenotable(): Boolean {
        return errorTypeConstructor.isDenotable
    }

    override fun getDeclarationDescriptor(): ClassifierDescriptor? {
        return errorTypeConstructor.declarationDescriptor
    }

    override fun toString(): String {
        return errorTypeConstructor.toString()
    }

    override fun getBuiltIns(): KotlinBuiltIns {
        return kotlinBuiltIns
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = this
}
