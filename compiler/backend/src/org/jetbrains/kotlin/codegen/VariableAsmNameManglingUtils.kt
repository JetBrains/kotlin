/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("VariableAsmNameManglingUtils")
package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.jvm.checkers.isValidDalvikCharacter

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

fun mangleNameIfNeeded(name: String): String {
    if (name.all { it.isValidCharacter() }) {
        return name
    }

    return buildString {
        for (c in name) {
            if (c.isValidCharacter()) {
                append(c)
            } else {
                val hexString = Integer.toHexString(c.toInt())
                assert(hexString.length <= 4)
                append("_u").append(hexString)
            }
        }
    }
}

private fun Char.isValidCharacter(): Boolean {
    return this != '$' && this != '-' && isValidDalvikCharacter(this)
}