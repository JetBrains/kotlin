/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import java.lang.reflect.Type

internal object ThrowingCaller : Caller<Nothing?> {
    override val member: Nothing?
        get() = null

    override val parameterTypes: List<Type>
        get() = emptyList()

    override val returnType: Type
        get() = Void.TYPE

    override fun call(args: Array<*>): Any? {
        throw UnsupportedOperationException("call/callBy are not supported for this declaration.")
    }
}
