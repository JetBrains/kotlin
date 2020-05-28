/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.evaluate

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

internal fun populateOperationsMap(
    unaryOperationsMap: ArrayList<Triple<String, List<KotlinType>, Boolean>>,
    binaryOperationsMap: ArrayList<Pair<String, List<KotlinType>>>,
    excludedFunctions: List<String>
) {
    val builtIns = DefaultBuiltIns.Instance

    @Suppress("UNCHECKED_CAST")
    val allPrimitiveTypes = builtIns.builtInsPackageScope.getContributedDescriptors()
        .filter { it is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(it.defaultType) } as List<ClassDescriptor>

    for (descriptor in allPrimitiveTypes + builtIns.string) {
        @Suppress("UNCHECKED_CAST")
        val functions = descriptor.getMemberScope(listOf()).getContributedDescriptors()
            .filter { it is CallableDescriptor && !excludedFunctions.contains(it.getName().asString()) } as List<CallableDescriptor>

        for (function in functions) {
            val parametersTypes = function.getParametersTypes()

            when (parametersTypes.size) {
                1 -> unaryOperationsMap.add(Triple(function.name.asString(), parametersTypes, function is FunctionDescriptor))
                2 -> binaryOperationsMap.add(function.name.asString() to parametersTypes)
                else -> throw IllegalStateException(
                    "Couldn't add following method from builtins to operations map: ${function.name} in class ${descriptor.name}"
                )
            }
        }
    }
}

private fun CallableDescriptor.getParametersTypes(): List<KotlinType> =
    listOf((containingDeclaration as ClassDescriptor).defaultType) +
            valueParameters.map { it.type.makeNotNullable() }
