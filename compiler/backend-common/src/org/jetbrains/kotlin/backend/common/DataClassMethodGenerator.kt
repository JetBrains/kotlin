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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.types.KotlinType

/**
 * A platform-independent logic for generating data class synthetic methods.
 * TODO: data class with zero components gets no toString/equals/hashCode methods. This is inconsistent and should be
 * changed here with the platform backends adopted.
 */
abstract class DataClassMethodGenerator(private val declaration: KtClassOrObject, private val bindingContext: BindingContext) {
    protected val classDescriptor: ClassDescriptor = BindingContextUtils.getNotNull(bindingContext, BindingContext.CLASS, declaration)

    fun generate() {
        generateComponentFunctionsForDataClasses()

        generateCopyFunctionForDataClasses(primaryConstructorParameters)

        val properties = dataProperties
        if (properties.isNotEmpty()) {
            generateDataClassToStringIfNeeded(properties)
            generateDataClassHashCodeIfNeeded(properties)
            generateDataClassEqualsIfNeeded(properties)
        }
    }

    protected abstract fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor)

    protected abstract fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>)

    protected abstract fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>)

    protected abstract fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>)

    protected abstract fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>)

    private fun generateComponentFunctionsForDataClasses() {
        // primary constructor should exist for data classes
        // but when generating light-classes still need to check we have one
        val constructor = classDescriptor.unsubstitutedPrimaryConstructor ?: return

        for (parameter in constructor.valueParameters) {
            val function = bindingContext.get(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter)
            if (function != null) {
                generateComponentFunction(function, parameter)
            }
        }
    }

    private fun generateCopyFunctionForDataClasses(constructorParameters: List<KtParameter>) {
        val copyFunction = bindingContext.get(BindingContext.DATA_CLASS_COPY_FUNCTION, classDescriptor) ?: return
        generateCopyFunction(copyFunction, constructorParameters)
    }

    private fun generateDataClassToStringIfNeeded(properties: List<PropertyDescriptor>) {
        val function = getMemberToGenerate("toString", KotlinBuiltIns::isString, List<ValueParameterDescriptor>::isEmpty) ?: return
        generateToStringMethod(function, properties)
    }

    private fun generateDataClassHashCodeIfNeeded(properties: List<PropertyDescriptor>) {
        val function = getMemberToGenerate("hashCode", KotlinBuiltIns::isInt, List<ValueParameterDescriptor>::isEmpty) ?: return
        generateHashCodeMethod(function, properties)
    }

    private fun generateDataClassEqualsIfNeeded(properties: List<PropertyDescriptor>) {
        val function = getMemberToGenerate("equals", KotlinBuiltIns::isBoolean) { parameters ->
            parameters.size == 1 && KotlinBuiltIns.isNullableAny(parameters.first().type)
        } ?: return
        generateEqualsMethod(function, properties)
    }

    private val dataProperties: List<PropertyDescriptor>
        get() = primaryConstructorParameters
                .filter { it.hasValOrVar() }
                .map { bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it)!! }

    private val primaryConstructorParameters: List<KtParameter>
        get() = (declaration as? KtClass)?.getPrimaryConstructorParameters().orEmpty()

    // Returns the descriptor for a function (whose parameters match the given predicate) which should be generated in the data class.
    // Note that we always generate equals/hashCode/toString in data classes, unless that would lead to a JVM signature clash with
    // another method, which can only happen if the method is declared in the data class (manually or via delegation).
    // Also there are no hard asserts or assumptions because such methods are generated for erroneous code as well (in light classes mode).
    private fun getMemberToGenerate(
            name: String,
            isReturnTypeOk: (KotlinType) -> Boolean,
            areParametersOk: (List<ValueParameterDescriptor>) -> Boolean
    ): FunctionDescriptor? =
            classDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .singleOrNull { function ->
                        function.kind.let { kind -> kind == SYNTHESIZED || kind == FAKE_OVERRIDE } &&
                        function.modality != Modality.FINAL &&
                        areParametersOk(function.valueParameters) &&
                        function.returnType != null &&
                        isReturnTypeOk(function.returnType!!)
                    }
}
