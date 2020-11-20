/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("VariableAsmNameManglingUtils")
package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl

const val DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX = "\$dstr\$"

fun getNameForDestructuredParameterOrNull(valueParameterDescriptor: ValueParameterDescriptor): String? {
    val variables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(valueParameterDescriptor) ?: return null

    @Suppress("SpellCheckingInspection")
    return DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX + variables.joinToString(separator = "$") { descriptor ->
        val name = descriptor.name
        mangleNameIfNeeded(
            when {
                name.isSpecial -> "\$_\$"
                else -> descriptor.name.asString()
            }
        )
    }
}
