/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.state.State
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor

data class Variable(val descriptor: DeclarationDescriptor, var state: State) {
    override fun toString(): String {
        val descriptorName = when (descriptor) {
            is ReceiverParameterDescriptor -> descriptor.containingDeclaration.name.toString() + "::this"
            else -> descriptor.name
        }
        return "Variable(descriptor=$descriptorName, state=$state)"
    }
}