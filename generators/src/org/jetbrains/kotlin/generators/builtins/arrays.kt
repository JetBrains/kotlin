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

package org.jetbrains.kotlin.generators.builtins.arrays

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import java.io.PrintWriter

class GenerateArrays(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin"

    override fun generateBody() {
        for (kind in PrimitiveType.values()) {
            val typeLower = kind.name().toLowerCase()
            val s = kind.capitalized
            val defaultValue = when(kind) { PrimitiveType.BOOLEAN -> "false"; else -> "zero" }
            out.println("/**")
            out.println(" * An array of ${typeLower}s. When targeting the JVM, instances of this class are represented as `${typeLower}[]`.")
            out.println(" * @constructor Creates a new array of the specified [size], with all elements initialized to ${defaultValue}.")
            out.println(" */")
            out.println("public class ${s}Array(size: Int) : Cloneable {")
            out.println("    /** Returns the array element at the given [index]. This method can be called using the index operator. */")
            out.println("    public fun get(index: Int): $s")
            out.println("    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */")
            out.println("    public fun set(index: Int, value: $s): Unit")
            out.println()
            out.println("    /** Returns the number of elements in the array. */")
            out.println("    public fun size(): Int")
            out.println()
            out.println("    /** Creates an iterator over the elements of the array. */")
            out.println("    public fun iterator(): ${s}Iterator")
            out.println()
            out.println("    public override fun clone(): ${s}Array")
            out.println("}")
            out.println()
        }
    }
}
