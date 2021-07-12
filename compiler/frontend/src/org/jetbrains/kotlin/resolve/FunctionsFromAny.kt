/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.util.OperatorNameConventions

object FunctionsFromAny {
    val EQUALS_METHOD_NAME = OperatorNameConventions.EQUALS
    val HASH_CODE_METHOD_NAME = Name.identifier("hashCode")
    val TO_STRING_METHOD_NAME = Name.identifier("toString")

    fun addFunctionFromAnyIfNeeded(
        thisDescriptor: ClassDescriptor,
        result: MutableCollection<SimpleFunctionDescriptor>,
        name: Name,
        fromSupertypes: Collection<SimpleFunctionDescriptor>,
    ) {
        if (shouldAddEquals(name, result, fromSupertypes)) {
            result.add(createEqualsFunctionDescriptor(thisDescriptor))
        }

        if (shouldAddHashCode(name, result, fromSupertypes)) {
            result.add(createHashCodeFunctionDescriptor(thisDescriptor))
        }

        if (shouldAddToString(name, result, fromSupertypes)) {
            result.add(createToStringFunctionDescriptor(thisDescriptor))
        }
    }

    fun shouldAddEquals(
        name: Name,
        declaredFunctions: Collection<SimpleFunctionDescriptor>,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    ): Boolean {
        return name == EQUALS_METHOD_NAME && shouldAddFunctionFromAny(
            declaredFunctions,
            fromSupertypes
        ) { function ->
            val parameters = function.valueParameters
            parameters.size == 1 && KotlinBuiltIns.isNullableAny(parameters.first().type)
        }
    }

    fun shouldAddHashCode(
        name: Name,
        declaredFunctions: Collection<SimpleFunctionDescriptor>,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    ): Boolean {
        return name == HASH_CODE_METHOD_NAME && shouldAddFunctionFromAny(
            declaredFunctions,
            fromSupertypes
        ) {
            it.valueParameters.isEmpty()
        }
    }

    fun shouldAddToString(
        name: Name,
        declaredFunctions: Collection<SimpleFunctionDescriptor>,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    ): Boolean {
        return name == TO_STRING_METHOD_NAME && shouldAddFunctionFromAny(
            declaredFunctions,
            fromSupertypes
        ) {
            it.valueParameters.isEmpty()
        }
    }

    fun createEqualsFunctionDescriptor(classDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
        doCreateFunctionFromAny(classDescriptor, EQUALS_METHOD_NAME)

    fun createHashCodeFunctionDescriptor(classDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
        doCreateFunctionFromAny(classDescriptor, HASH_CODE_METHOD_NAME)

    fun createToStringFunctionDescriptor(classDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
        doCreateFunctionFromAny(classDescriptor, TO_STRING_METHOD_NAME)

    private fun doCreateFunctionFromAny(classDescriptor: ClassDescriptor, name: Name): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            classDescriptor, Annotations.EMPTY, name, CallableMemberDescriptor.Kind.SYNTHESIZED, classDescriptor.source
        )

        val functionFromAny = classDescriptor.builtIns.any.getMemberScope(emptyList())
            .getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS).single()

        functionDescriptor.initialize(
            null,
            classDescriptor.thisAsReceiverParameter,
            emptyList(),
            functionFromAny.typeParameters,
            functionFromAny.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
            functionFromAny.returnType,
            Modality.OPEN,
            DescriptorVisibilities.PUBLIC
        )

        return functionDescriptor
    }

    private fun shouldAddFunctionFromAny(
        declaredFunctions: Collection<SimpleFunctionDescriptor>,
        fromSupertypes: Collection<SimpleFunctionDescriptor>,
        checkParameters: (FunctionDescriptor) -> Boolean
    ): Boolean {
        // Add 'equals', 'hashCode', 'toString' iff there is no such declared member AND there is no such final member in supertypes
        return declaredFunctions.none(checkParameters) &&
                fromSupertypes.none { checkParameters(it) && it.modality == Modality.FINAL }
    }
}
