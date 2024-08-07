/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.org.objectweb.asm.Opcodes

//class AsJavaAtomic : IntrinsicMethod() {
//    override fun toCallable(method: CallableMethod): Callable =
//        createIntrinsicCallable(method) {
//            it.visitTypeInsn(Opcodes.CHECKCAST, "java/util/concurrent/atomic/AtomicInteger")
//        }
//}
//
//class AsKotlinAtomic : IntrinsicMethod() {
//    override fun toCallable(method: CallableMethod): Callable =
//        createIntrinsicCallable(method) {
//            it.visitTypeInsn(Opcodes.CHECKCAST, "kotlin/concurrent/AtomicInt")
//        }
//}