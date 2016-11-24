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

package kotlin.coroutines

/**
 * This function allows to obtain a continuation instance inside the suspend functions.
 * As a suspend function may be only tail-called inside another suspend function, this call will be used as a return value of the latter one.
 *
 * If the [body] returns the [Suspend] object, that means that suspend-function cannot return a value immediately,
 * i.e. it's literally suspends continuation, and the continuation will be resumed at some moment in the future.
 *
 * Otherwise return value must have a type assignable to 'T'.
 * As the latter part cannot be correctly typechecked, it remains on the conscience of the suspend function's author.
 *
 * Note that it's not recommended to call a [Continuation] method in the same stackframe where suspension function is run,
 * they should be called asynchronously later (probably from the different thread).
 *
 */
@SinceKotlin("1.1")
public inline suspend fun <T> suspendWithCurrentContinuation(body: (Continuation<T>) -> Any?): T
