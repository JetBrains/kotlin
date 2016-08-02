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

package org.jetbrains.kotlin.ir

typealias SourceLocation = Long

fun SourceLocation(index: Int, offset: Int) =
        (index.toLong() shl 32) + offset.toLong()

const val NO_LOCATION: SourceLocation = -1L
const val UNDEFINED_INDEX: Int = -1
const val UNDEFINED_OFFSET: Int = -1

val SourceLocation.fileIndex: Int
    get() = if (this == NO_LOCATION) UNDEFINED_INDEX else (this ushr 32).toInt()

val SourceLocation.fileOffset: Int
    get() = if (this == NO_LOCATION) UNDEFINED_OFFSET else (this and 0xFFFFFFFFL).toInt()
