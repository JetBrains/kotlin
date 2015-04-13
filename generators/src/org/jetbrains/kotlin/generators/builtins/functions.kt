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

package org.jetbrains.kotlin.generators.builtins.functions

import org.jetbrains.kotlin.generators.builtins.functions.FunctionKind.*
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

val MAX_PARAM_COUNT = 22

enum class FunctionKind(
        private val classNamePrefix: String,
        val docPrefix: String,
        val hasReceiverParameter: Boolean,
        private val superClassNamePrefix: String?
) {
    FUNCTION("Function", "A function", false, null),
    EXTENSION_FUNCTION("ExtensionFunction", "An extension function", true, null),

    K_FUNCTION("KFunction", "A function with introspection capabilities", false, "Function"),
    K_MEMBER_FUNCTION("KMemberFunction", "A member function with introspection capabilities", true, "ExtensionFunction"),
    K_EXTENSION_FUNCTION("KExtensionFunction", "An extension function with introspection capabilities", true, "ExtensionFunction");

    fun getFileName() = (if (isReflection()) "reflect/" else "") + classNamePrefix + "s.kt"
    fun getClassName(i: Int) = classNamePrefix + i
    fun getSuperClassName(i: Int) = superClassNamePrefix?.plus(i)
    fun getPackage() = if (isReflection()) "kotlin.reflect" else "kotlin"

    fun isReflection() = name() startsWith "K"
}

class GenerateFunctions(out: PrintWriter, val kind: FunctionKind) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = kind.getPackage()

    fun generateTypeParameters(i: Int, variance: Boolean) {
        out.print("<")
        if (kind.hasReceiverParameter) {
            if (variance) out.print("in ")
            out.print("T, ")
        }

        for (j in 1..i) {
            if (variance) out.print("in ")
            out.print("P$j, ")
        }

        generateReturnTypeParameter(variance)

        out.print(">")
    }

    fun generateReturnTypeParameter(variance: Boolean) {
        if (variance) out.print("out ")
        out.print("R")
    }

    override fun generateBody() {
        for (i in 0..MAX_PARAM_COUNT) {
            generateDocumentation(i)
            out.print("public interface " + kind.getClassName(i))
            generateTypeParameters(i, variance = true)
            generateSuperClass(i)
            generateFunctionClassBody(i)
        }
    }

    fun generateDocumentation(i: Int) {
        val suffix = if (i == 1) "" else "s"
        out.println("/** ${kind.docPrefix} that takes $i argument${suffix}. */")
    }

    fun generateSuperClass(i: Int) {
        val superClass = kind.getSuperClassName(i)
        if (superClass != null) {
            out.print(" : $superClass")
            generateTypeParameters(i, variance = false)
        }
        else {
            out.print(" : Function<")
            generateReturnTypeParameter(variance = false)
            out.print(">")
        }
    }

    fun generateFunctionClassBody(i: Int): Unit = when (kind) {
        FUNCTION, EXTENSION_FUNCTION -> {
            out.println(" {")
            generateInvokeSignature(i)
            out.println("}")
        }
        K_FUNCTION, K_MEMBER_FUNCTION, K_EXTENSION_FUNCTION -> {
            out.println()
        }
    }

    fun generateInvokeSignature(i: Int) {
        if (i == 0) {
            out.println("    /** Invokes the function. */")
        } else {
            val suffix = if (i == 1) "" else "s"
            out.println("    /** Invokes the function with the specified argument${suffix}. */")
        }
        out.print("    public fun ${if (kind.hasReceiverParameter) "T." else ""}invoke(")
        for (j in 1..i) {
            out.print("p$j: P$j")
            if (j < i) {
                out.print(", ")
            }
        }
        out.println("): R")
    }
}
