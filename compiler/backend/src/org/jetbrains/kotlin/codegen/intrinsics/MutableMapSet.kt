/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.org.objectweb.asm.Type

/**
 * This intrinsic mitigates issue KT-14227.
 *
 * `MutableMap.get` is an instance method, while `MutableMap.set` is an inline-only extension function from kotlin stdlib.
 * This confuses very adhoc-ish code written in "old" JVM back-end for augmented assignment and increment/decrement operations desugaring,
 * which produces incorrect bytecode for such rather trivial constructs.
 *
 * Fixing it in general case requires some design decisions for corner cases with arbitrary argument shapes and types for `get` and `set`.
 */
class MutableMapSet : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable =
        object : IntrinsicCallable(
            method,
            { v ->
                v.invokeinterface("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                v.pop()
            }
        ) {
            override val parameterTypes: Array<Type>
                get() = method.valueParameterTypes.toTypedArray()
        }
}