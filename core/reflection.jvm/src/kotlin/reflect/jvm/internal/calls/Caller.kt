/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import java.lang.reflect.Member
import java.lang.reflect.Type

internal interface Caller<out M : Member?> {
    val member: M

    val returnType: Type

    val parameterTypes: List<Type>

    fun checkArguments(args: Array<*>) {
        checkArguments(args.size)
    }

    fun checkArguments(argsCount: Int) {
        if (arity != argsCount) {
            throw IllegalArgumentException("Callable expects $arity arguments, but $argsCount were provided.")
        }
    }

    fun call(args: Array<*>): Any?

    /**
     * @see [CallerImpl.Method.BoundStatic.isCallByToValueClassMangledMethod]
     */
    val isBoundInstanceCallWithValueClasses: Boolean
        get() = false
}

internal val Caller<*>.arity: Int
    get() = parameterTypes.size

/**
 * A marker interface that signifies that this caller has a "bound receiver" object which should be used as the dispatch receiver instance.
 */
interface BoundCaller
