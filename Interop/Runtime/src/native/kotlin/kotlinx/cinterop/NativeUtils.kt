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

import kotlin.native.internal.Intrinsic

internal fun decodeFromUtf8(bytes: ByteArray): String = bytes.stringFromUtf8()

fun encodeToUtf8(str: String): ByteArray = str.toUtf8()

@Intrinsic
external fun bitsToFloat(bits: Int): Float

@Intrinsic
external fun bitsToDouble(bits: Long): Double

// TODO: deprecate.
@Intrinsic
external fun <R : Number> Number.signExtend(): R

// TODO: deprecate.
@Intrinsic
external fun <R : Number> Number.narrow(): R

@Intrinsic external fun <R : Any> Byte.convert(): R
@Intrinsic external fun <R : Any> Short.convert(): R
@Intrinsic external fun <R : Any> Int.convert(): R
@Intrinsic external fun <R : Any> Long.convert(): R
@Intrinsic external fun <R : Any> UByte.convert(): R
@Intrinsic external fun <R : Any> UShort.convert(): R
@Intrinsic external fun <R : Any> UInt.convert(): R
@Intrinsic external fun <R : Any> ULong.convert(): R

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmName(val name: String)