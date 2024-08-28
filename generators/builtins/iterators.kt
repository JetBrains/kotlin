/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.builtins.iterators

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import java.io.PrintWriter

class GenerateIterators(out: PrintWriter) : BuiltInsSourceGenerator(out, annotateAsBuiltin = true) {
    override fun getPackage() = "kotlin.collections"
    override fun generateBody() {
        for (kind in PrimitiveType.entries) {
            val type = kind.capitalized
            out.println("""
/**
 * An iterator over a sequence of values of type `${type}`.
 *
 * This is a substitute for `Iterator<${type}>` that provides a specialized version of `next(): T` method: `next${type}(): $type`
 * and has a special handling by the compiler to avoid platform-specific boxing conversions as a performance optimization.
 *
 * In the following example:
 *
 * ```kotlin
 * class ${type}Container(private val data: ${type}Array) {
 *
 *     // ${type}Iterator instead of Iterator<${type}> in the signature
 *     operator fun iterator(): ${type}Iterator = object : ${type}Iterator() {
 *         private var idx = 0
 *
 *         override fun next${type}(): $type {
 *             if (!hasNext()) throw NoSuchElementException()
 *             return data[idx++]
 *         }
 *
 *         override fun hasNext(): Boolean = idx < data.size
 *     }
 * }
 *
 * for (element in ${type}Container(${kind.arraySample()})) {
 *     ... handle element ...
 * }
 * ```
 * No boxing conversion is performed during the for-loop iteration.
 * Note that the iterator itself will still be allocated.
 */
            """.trimIndent())
            out.println("public abstract class ${type}Iterator : Iterator<$type> {")
            out.println("    final override fun next(): $type = next$type()")
            out.println()
            out.println("""
    /**
     * Returns the next element in the iteration without boxing conversion.
     * @throws NoSuchElementException if the iteration has no next element.
     */""")
            out.println("    public abstract fun next$type(): $type")
            out.println("}")
            out.println()
        }
    }

    private fun PrimitiveType.arraySample(): String {
        return when (this) {
            PrimitiveType.BYTE -> "byteArrayOf(1, 2, 3)"
            PrimitiveType.CHAR -> "charArrayOf('1', '2', '3')"
            PrimitiveType.SHORT -> "shortArrayOf(1, 2, 3)"
            PrimitiveType.INT -> "intArrayOf(1, 2, 3)"
            PrimitiveType.LONG -> "longArrayOf(1, 2, 3)"
            PrimitiveType.FLOAT -> "floatArrayOf(1f, 2f, 3f)"
            PrimitiveType.DOUBLE -> "doubleArrayOf(1.0, 2.0, 3.0)"
            PrimitiveType.BOOLEAN -> "booleanArrayOf(true, false, true)"
        }
    }
}
