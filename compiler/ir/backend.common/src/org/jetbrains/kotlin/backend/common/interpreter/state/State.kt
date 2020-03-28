/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.state

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.backend.common.interpreter.stack.Variable
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction

interface State {
    val fields: MutableList<Variable>
    val irClass: IrClass

    fun getState(descriptor: DeclarationDescriptor): State? {
        return fields.firstOrNull { it.descriptor.equalTo(descriptor) }?.state
    }

    fun setState(newVar: Variable)

    /**
     * This method is used for passing a copy of a state.
     * It is necessary then copy change its state's value, but the original one must remain the same.
     *
     * @see copyReceivedValue.kt
     * @see tryFinally.kt
     */
    fun copy(): State

    fun getIrFunction(descriptor: FunctionDescriptor): IrFunction?
}
