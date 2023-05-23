/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import java.io.File

class GeneratedFile(val file: File, val newText: String)

class Import(val packageName: String, val className: String)

fun ClassName.parameterizedByIfAny(typeArguments: List<TypeName>) =
    if (typeArguments.isNotEmpty()) parameterizedBy(typeArguments) else this

fun TypeName.tryParameterizedBy(vararg typeArguments: TypeName) = when (this) {
    is ClassName -> parameterizedBy(*typeArguments)
    is ParameterizedTypeName -> this.rawType.parameterizedBy(*typeArguments)
    else -> {
        if (typeArguments.isNotEmpty())
            error("Type $this cannot be parameterized")
        else this
    }
}

fun code(code: String, vararg args: Any?) = CodeBlock.of(code, *args)
