/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object JsAnnotations {
    val jsModuleFqn = FqName("kotlin.js.JsModule")
    val jsNonModuleFqn = FqName("kotlin.js.JsNonModule")
    val jsNameFqn = FqName("kotlin.js.JsName")
    val jsQualifierFqn = FqName("kotlin.js.JsQualifier")
    val jsExportFqn = FqName("kotlin.js.JsExport")
}

@Suppress("UNCHECKED_CAST")
fun IrConstructorCall.getSingleConstStringArgument() =
    (getValueArgument(0) as IrConst<String>).value

fun IrAnnotationContainer.getJsModule(): String? =
    getAnnotation(JsAnnotations.jsModuleFqn)?.getSingleConstStringArgument()

fun IrAnnotationContainer.isJsNonModule(): Boolean =
    hasAnnotation(JsAnnotations.jsNonModuleFqn)

fun IrAnnotationContainer.getJsQualifier(): String? =
    getAnnotation(JsAnnotations.jsQualifierFqn)?.getSingleConstStringArgument()

fun IrAnnotationContainer.getJsName(): String? =
    getAnnotation(JsAnnotations.jsNameFqn)?.getSingleConstStringArgument()

fun IrAnnotationContainer.isJsExport(): Boolean =
    hasAnnotation(JsAnnotations.jsExportFqn)

fun IrDeclarationWithName.getJsNameOrKotlinName(): Name =
    when (val jsName = getJsName()) {
        null -> name
        else -> Name.identifier(jsName)
    }