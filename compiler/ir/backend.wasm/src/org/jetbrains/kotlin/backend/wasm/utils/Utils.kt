/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isFunctionMarker
import java.util.Collections
import java.util.IdentityHashMap

// Backed codegen can only handle try/catch in the canonical form.
// The defined for Wasm backend canonical form of try/catch:
// try {
//   ...exprs
// }
// catch (e: Throwable) {
//   ...exprs
// }
// no-finally
internal fun IrTry.isCanonical(context: WasmBackendContext) =
    catches.size <= 1 &&
            catches.all { it.catchParameter.type == context.irBuiltIns.throwableType } &&
            finallyExpression == null

internal val IrClass.isAbstractOrSealed
    get() = modality == Modality.ABSTRACT || modality == Modality.SEALED

internal fun getFunctionalInterfaceSlot(iFace: IrClass): Int {
    check(iFace.symbol.isFunction())
    return if (iFace.defaultType.isFunctionMarker()) return 0 else iFace.typeParameters.size
}

internal val String.fitsLatin1
    get() = this.all { it.code in 0..255 }

fun <T> identityHashSetOf(): MutableSet<T> =
    Collections.newSetFromMap(IdentityHashMap<T, Boolean>())

fun <T> identityHashSetOf(expectedMaxSize: Int): MutableSet<T> =
    Collections.newSetFromMap(IdentityHashMap<T, Boolean>(expectedMaxSize))

