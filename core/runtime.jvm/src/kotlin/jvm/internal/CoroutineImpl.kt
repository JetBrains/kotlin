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

abstract class CoroutineImpl(arity: Int) : Lambda(arity), Continuation<Any?> {
    @JvmField
    protected var _controller: Any? = null

    // It's not protected because can be used from noinline lambdas inside coroutine (when calling non-suspend functions)
    // Also there might be needed a way to access a controller by Continuation instance when it's inherited from CoroutineImpl
    val controller: Any? get() = _controller

    // Any label state less then zero indicates that coroutine is not run and can't be resumed in any way.
    // Specific values do not matter by now, but currently -2 used for uninitialized coroutine (no controller is assigned),
    // and -1 will mean that coroutine execution is over (does not work yet).
    @JvmField
    protected var label: Int = -2

    override fun resume(data: Any?) {
        doResume(data, null)
    }

    override fun resumeWithException(exception: Throwable) {
        doResume(null, exception)
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?)
}
