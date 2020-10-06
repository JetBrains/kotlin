/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object JsAnnotations {
    val jsModuleFqn = FqName("kotlin.js.JsModule")
    val jsNonModuleFqn = FqName("kotlin.js.JsNonModule")
    val jsNameFqn = FqName("kotlin.js.JsName")
    val jsQualifierFqn = FqName("kotlin.js.JsQualifier")
    val jsExportFqn = FqName("kotlin.js.JsExport")
    val jsNativeGetter = FqName("kotlin.js.nativeGetter")
    val jsNativeSetter = FqName("kotlin.js.nativeSetter")
    val jsNativeInvoke = FqName("kotlin.js.nativeInvoke")
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

fun IrAnnotationContainer.isJsNativeGetter(): Boolean = hasAnnotation(JsAnnotations.jsNativeGetter)

fun IrAnnotationContainer.isJsNativeSetter(): Boolean = hasAnnotation(JsAnnotations.jsNativeSetter)

fun IrAnnotationContainer.isJsNativeInvoke(): Boolean = hasAnnotation(JsAnnotations.jsNativeInvoke)

fun IrDeclarationWithName.getJsNameOrKotlinName(): Name =
    when (val jsName = getJsName()) {
        null -> name
        else -> Name.identifier(jsName)
    }

private val associatedObjectKeyAnnotationFqName = FqName("kotlin.reflect.AssociatedObjectKey")

val IrClass.isAssociatedObjectAnnotatedAnnotation: Boolean
    get() = isAnnotationClass && annotations.any { it.symbol.owner.constructedClass.fqNameWhenAvailable == associatedObjectKeyAnnotationFqName }

fun IrConstructorCall.associatedObject(): IrClass? {
    if (!symbol.owner.constructedClass.isAssociatedObjectAnnotatedAnnotation) return null
    val klass = ((getValueArgument(0) as? IrClassReference)?.symbol as? IrClassSymbol)?.owner ?: return null
    return if (klass.isObject) klass else null
}
