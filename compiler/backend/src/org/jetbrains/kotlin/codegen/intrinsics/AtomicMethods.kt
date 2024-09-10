/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type.INT_TYPE

class AtomicIntCompareAndExchange : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable =
        createIntrinsicCallable(method) { v ->
            assert(method.getValueParameters().size == 2)
            v.load(0, OBJECT_TYPE)
            v.load(1, INT_TYPE)
            v.load(2, INT_TYPE)
            v.invokevirtual("kotlin/concurrent/Atomics_jvmKt", "myCompareAndExchange", "(Ljava/util/concurrent/atomic/AtomicInteger;II)I", false)
//            assert(method.getValueParameters().size == 2)
//            val ifCasFailed = Label()
//            val ifNewValueDoesNotEqualExpected = Label()
//            v.mark(ifNewValueDoesNotEqualExpected)
//            v.load(0, OBJECT_TYPE)
//            v.load(1, INT_TYPE)
//            v.load(2, INT_TYPE)
//            v.invokevirtual("java/util/concurrent/atomic/AtomicInteger", "compareAndSet", "(II)Z", false)
//            v.ifeq(ifCasFailed)
//            v.load(1, INT_TYPE)
//            v.ret(1)
//            v.mark(ifCasFailed)
//            v.load(0, OBJECT_TYPE)
//            v.invokevirtual("java/util/concurrent/atomic/AtomicInteger", "get", "()I", false)
//            v.store(3, INT_TYPE)
//            v.load(3, INT_TYPE)
//            v.load(1, INT_TYPE)
//            v.ificmpeq(ifNewValueDoesNotEqualExpected)
//            v.load(3, INT_TYPE)
//            v.ret(3)
        }
}