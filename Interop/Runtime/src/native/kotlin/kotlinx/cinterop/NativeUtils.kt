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

import konan.internal.Intrinsic

internal fun decodeFromUtf8(bytes: ByteArray): String = bytes.stringFromUtf8()

fun encodeToUtf8(str: String): ByteArray = str.toUtf8()

@Intrinsic
external fun bitsToFloat(bits: Int): Float

@Intrinsic
external fun bitsToDouble(bits: Long): Double

@Intrinsic
external fun <R : Number> Number.signExtend(): R

@Intrinsic
external fun <R : Number> Number.narrow(): R

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmName(val name: String)