/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

abstract class ScriptLikeContext(
    val typeMapper: KotlinTypeMapper,
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?
) : ClassContext(typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null) {
    abstract fun getOuterReceiverExpression(prefix: StackValue?, thisOrOuterClass: ClassDescriptor): StackValue

    open fun captureVariable(closure: MutableClosure, target: DeclarationDescriptor): StackValue? {
        return null
    }
}