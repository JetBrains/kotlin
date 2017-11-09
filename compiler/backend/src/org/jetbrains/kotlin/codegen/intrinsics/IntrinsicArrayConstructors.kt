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

@file:Suppress("unused", "UNCHECKED_CAST")

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

// Bodies of methods in this file are used in InlineCodegen to inline array constructors. It is loaded via reflection at runtime.
// TODO: generate the bytecode manually instead, do not depend on the previous compiler working correctly here

internal val classId: ClassId =
        ClassId.topLevel(FqName("org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt"))

internal val bytecode: ByteArray by lazy {
    val stream = object {}::class.java.classLoader.getResourceAsStream("${classId.asString()}.class")
    stream.readBytes().apply {
        stream.close()
    }
}

private inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>

private inline fun <reified T> arrayOf(vararg elements: T): Array<T> = elements as Array<T>

private inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
    val result = arrayOfNulls<T>(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result as Array<T>
}

private inline fun DoubleArray(size: Int, init: (Int) -> Double): DoubleArray {
    val result = DoubleArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun FloatArray(size: Int, init: (Int) -> Float): FloatArray {
    val result = FloatArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun LongArray(size: Int, init: (Int) -> Long): LongArray {
    val result = LongArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun IntArray(size: Int, init: (Int) -> Int): IntArray {
    val result = IntArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun CharArray(size: Int, init: (Int) -> Char): CharArray {
    val result = CharArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun ShortArray(size: Int, init: (Int) -> Short): ShortArray {
    val result = ShortArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun ByteArray(size: Int, init: (Int) -> Byte): ByteArray {
    val result = ByteArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}

private inline fun BooleanArray(size: Int, init: (Int) -> Boolean): BooleanArray {
    val result = BooleanArray(size)
    for (i in result.indices) {
        result[i] = init(i)
    }
    return result
}
