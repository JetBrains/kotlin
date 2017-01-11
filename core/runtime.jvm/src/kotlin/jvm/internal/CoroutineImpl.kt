/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.jvm.internal

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.SUSPENDED_MARKER

abstract class CoroutineImpl(
        arity: Int,
        @JvmField
        protected var completion: Continuation<Any?>?
) : DispatchedContinuation<Any?>, Lambda(arity), Continuation<Any?>  {

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    @JvmField
    protected var label: Int = if (completion != null) 0 else -1

    private val _dispatcher: ContinuationDispatcher? = (completion as? DispatchedContinuation<*>)?.dispatcher

    override val dispatcher: ContinuationDispatcher?
        get() = _dispatcher

    private var facade_: Continuation<Any?>? = null
    val facade: Continuation<Any?> get() {
        if (facade_ == null) {
            facade_ = wrapContinuationIfNeeded(this, dispatcher)
        }

        return facade_!!
    }

    override fun resume(value: Any?) {
        try {
            val result = doResume(value, null)
            if (result != SUSPENDED_MARKER)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        try {
            val result = doResume(null, exception)
            if (result != SUSPENDED_MARKER)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?): Any?
}

internal interface DispatchedContinuation<in T> : Continuation<T> {
    val dispatcher: ContinuationDispatcher?
}
