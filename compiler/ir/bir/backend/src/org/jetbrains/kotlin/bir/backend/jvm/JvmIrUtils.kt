/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

val BirClass.isJvmInterface: Boolean
    get() = kind == ClassKind.ANNOTATION_CLASS || kind == ClassKind.INTERFACE

fun BirDeclaration.getJvmNameFromAnnotation(jvmNameAnnotation: BirConstructorCall): String? {
    // TODO lower @JvmName?
    val const = jvmNameAnnotation.valueArguments[0] as? BirConst<*> ?: return null
    val value = const.value as? String ?: return null
    return when (origin) {
        IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> "$value\$default"
        JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE,
        JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE -> "$value$FOR_INLINE_SUFFIX"
        else -> value
    }
}