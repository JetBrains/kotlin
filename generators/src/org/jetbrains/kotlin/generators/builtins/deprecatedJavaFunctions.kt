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

package org.jetbrains.kotlin.generators.builtins

import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

class GenerateDeprecatedJavaFunction(
        out: PrintWriter,
        private val arity: Int,
        private val extension: Boolean
) : BuiltInsSourceGenerator(out) {
    override val language = BuiltInsSourceGenerator.Language.JAVA

    override fun generateBody() {
        val params = (1..arity).map { "P$it" }
        val generics = "<" + ((if (extension) listOf("E") else listOf()) + params + listOf("R")).join() + ">"

        val name = if (extension) "ExtensionFunction" else "Function"
        val superArity = if (extension) arity + 1 else arity

        out.println("/**")
        out.println(" * @deprecated Use the new function classes from the package kotlin.jvm.functions.")
        out.println(" */")
        out.println("@Deprecated")
        out.println("public interface $name$arity$generics extends kotlin.jvm.functions.Function$superArity$generics {")
        out.println("}")
    }
}
