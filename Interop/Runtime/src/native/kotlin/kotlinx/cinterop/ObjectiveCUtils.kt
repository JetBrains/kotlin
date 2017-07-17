/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlinx.cinterop

inline fun <R> autoreleasepool(block: () -> R): R {
    val pool = objc_autoreleasePoolPush()
    return try {
        block()
    } finally {
        objc_autoreleasePoolPop(pool)
    }
}

// TODO: null checks
var <T : ObjCObject?> ObjCObjectVar<T>.value: T
    get() = interpretObjCPointerOrNull<T>(nativeMemUtils.getNativePtr(this)).uncheckedCast<T>()
    set(value) = nativeMemUtils.putNativePtr(this, value.rawPtr)