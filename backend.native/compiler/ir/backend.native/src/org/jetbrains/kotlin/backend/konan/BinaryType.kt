/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan

sealed class BinaryType<out T> {
    class Primitive(val type: PrimitiveBinaryType) : BinaryType<Nothing>()
    class Reference<T>(val types: Sequence<T>, val nullable: Boolean) : BinaryType<T>()
}

fun BinaryType<*>.primitiveBinaryTypeOrNull(): PrimitiveBinaryType? = when (this) {
    is BinaryType.Primitive -> this.type
    is BinaryType.Reference -> null
}

enum class PrimitiveBinaryType {
    BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, POINTER
}