/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeRuntimeNames

/**
 * A representation of `@BindReverseBridgeToMethod` annotation.
 *
 * @property annotationElement the original IR annotation
 * @property bridgeFunction the annotated function that serves as the reverse bridge
 * @property targetClass the Kotlin class whose method is being bridged
 * @property targetMethod the name of the method to create a reverse bridge for
 */
class BindReverseBridgeToMethod(
    val annotationElement: IrAnnotation,
    val bridgeFunction: IrSimpleFunction,
    val targetClass: IrClass,
    val targetMethod: String,
)

private fun IrAnnotation.bindReverseBridgeToMethod(bridgeFunction: IrSimpleFunction): BindReverseBridgeToMethod? {
    if (!isAnnotation(NativeRuntimeNames.Annotations.BindReverseBridgeToMethod.asSingleFqName())) {
        return null
    }
    val targetClass = (getValueArgument(Name.identifier("targetClass")) as IrClassReference).classType.getClass()!!
    val targetMethod = getAnnotationStringValue("targetMethod")
    return BindReverseBridgeToMethod(this, bridgeFunction, targetClass, targetMethod)
}

/**
 * Return a list of `@BindReverseBridgeToMethod` annotations from functions in this [IrFile].
 */
val IrFile.allBindReverseBridgeToMethod: List<BindReverseBridgeToMethod>
    get() = declarations
        .filterIsInstance<IrSimpleFunction>()
        .flatMap { function ->
            function.annotations.mapNotNull { it.bindReverseBridgeToMethod(function) }
        }
