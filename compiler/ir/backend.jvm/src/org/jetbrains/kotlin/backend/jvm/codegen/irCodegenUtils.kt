/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.FrameMapBase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.org.objectweb.asm.Type

class IrFrameMap : FrameMapBase<IrSymbol>()

internal val IrFunction.isStatic
    get() = (this.dispatchReceiverParameter == null && this !is IrConstructor) ||
            (parentAsClass.isObject && this.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME)) //TODO add lowering

fun IrFrameMap.enter(irDeclaration: IrSymbolOwner, type: Type): Int {
    return enter(irDeclaration.symbol, type)
}

fun IrFrameMap.leave(irDeclaration: IrSymbolOwner): Int {
    return leave(irDeclaration.symbol)
}

val IrClass.isJvmInterface get() = isAnnotationClass || isInterface