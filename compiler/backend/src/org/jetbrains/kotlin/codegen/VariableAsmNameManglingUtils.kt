/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("VariableAsmNameManglingUtils")
package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.jvm.checkers.isValidDalvikCharacter

fun getNameForDestructuredParameterOrNull(valueParameterDescriptor: ValueParameterDescriptor): String? {
    val variables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(valueParameterDescriptor) ?: return null

    @Suppress("SpellCheckingInspection")
    return "\$dstr\$" + variables.joinToString(separator = "$") { descriptor ->
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
                append('-').append(c.toHex())
            }
        }
    }
}

private fun Char.toHex(): String {
    val hexString = Integer.toHexString(this.toInt())
    assert(hexString.length <= 4)
    return if (hexString.length == 4) {
        hexString
    } else {
        "0".repeat(4 - hexString.length) + hexString
    }
}

private fun Char.isValidCharacter(): Boolean {
    return this != '$' && this != '-' && isValidDalvikCharacter(this)
}