/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

interface JvmIrIntrinsicExtension : IrIntrinsicExtension {
    /**
     * Returns [IntrinsicMethod] that should emit specific bytecode instead of a regular bytecode for calling [symbol],
     * or `null` if [symbol]'s call should not be replaced.
     */
    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod?

    /**
     * Allows to process plugin-defined reified operation marker.
     * Reified operation marker is an INVOKESTATIC call to IntrinsicsSupport.reifiedOperationMarker(operationType, typeVariableName) followed
     * by actual operation to be reified.
     * For marker to be determined as plugin-defined, another special call should be inserted directly afterwards the operation.
     * This call is MagicApiIntrinsics.voidMagicApiCall(object). Object should be a string loaded by LDC instruction. Contents of the string is plugin-defined
     * and recommended way to pass data to a plugin.
     *
     * This function must return `true` if marker was processed.
     * If this method returns `false`, other plugins would be queried, and if others also return `false`, a regular intrinsic determined by operationType would be inserted.
     *
     * If marker was processed, this is plugin's responsibility to remove any calls to MagicApiIntrinsics.voidMagicApiCall and its arguments.
     *
     * Example of plugin-defined reified operation marker:
     *
     * ```
     * iconst(6) // operationType=6
     * aconst(T) // typeParamName=T
     * invokestatic(kotlin/jvm/internal/Intrinsics.reifiedOperationMarker)
     * aconst(null) // This is operation to be reified. In case of operationType=6, this is typeOf<T>().
     *              // A KType instance would normally be generated on stack instead of null.
     * aconst("pluginDataString") // arbitrary constant string
     * invokestatic(kotlin/jvm/internal/MagicApiIntrinsics.voidMagicApiCall(Ljava/lang/Object;)V) // plugin marker call
     * ```
     *
     * Such approach with two markers was chosen mainly for compatibility reasons:
     * Call to voidMagicApiCall should be inserted directly after operationType-specific instruction (aconst(null) in the example with typeOf).
     * If we form bytecode this way, old compilers would be able to correctly inline normal reified operation here, even if they do not know anything about plugin reifications.
     * They won't remove invokestatic(voidMagicApiCall), but it is not a problem, since kotlin-stdlib has this function, and it is a no-op.
     */
    fun rewritePluginDefinedOperationMarker(
        v: InstructionAdapter,
        reifiedInsn: AbstractInsnNode,
        instructions: InsnList,
        type: IrType
    ): Boolean
}