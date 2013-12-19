/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.runtime.functions

import java.io.PrintWriter
import org.jetbrains.jet.generators.runtime.functions.FunctionKind.*
import org.jetbrains.jet.generators.runtime.*

val MAX_PARAM_COUNT = 22

enum class FunctionKind(
        private val classNamePrefix: String,
        val hasReceiverParameter: Boolean,
        private val superClassNamePrefix: String?
) {
    FUNCTION : FunctionKind("Function", false, null)
    EXTENSION_FUNCTION : FunctionKind("ExtensionFunction", true, null)
    K_FUNCTION : FunctionKind("KFunction", false, "Function")
    K_MEMBER_FUNCTION : FunctionKind("KMemberFunction", true, "ExtensionFunction")
    K_EXTENSION_FUNCTION : FunctionKind("KExtensionFunction", true, "ExtensionFunction")

    fun getFileName() = classNamePrefix + "s.kt"
    fun getImplFileName() = classNamePrefix + "sImpl.kt"
    fun getClassName(i: Int) = classNamePrefix + i
    fun getImplClassName(i: Int) = classNamePrefix + "Impl" + i
    fun getSuperClassName(i: Int) = superClassNamePrefix?.plus(i)
}

class GenerateFunctions(val out: PrintWriter, val kind: FunctionKind) {
    fun generateFunctionClasses() {
        generatedBy(out, javaClass.getName())
        for (i in 0..MAX_PARAM_COUNT) {
            out.print("public trait " + kind.getClassName(i))
            generateTypeParameters(i, true)
            generateSuperClass(i)
            generateFunctionClassBody(i)
        }
    }

    fun generateFunctionImplClasses() {
        generatedBy(out, javaClass.getName())
        for (i in 0..MAX_PARAM_COUNT) {
            out.print("public abstract class " + kind.getImplClassName(i))
            generateTypeParameters(i, true)
            out.print(" : ")
            out.print(kind.getClassName(i))
            generateTypeParameters(i, false)
            out.println(", DefaultJetObject() {")
            generateToStringForFunctionImpl()
            out.println("}")
        }
    }

    fun generateSuperClass(i: Int) {
        val superClass = kind.getSuperClassName(i)
        if (superClass != null) {
            out.print(" : $superClass")
            generateTypeParameters(i, false)
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
        else -> {
            throw IllegalStateException("Unknown kind: " + kind)
        }
    }

    fun generateInvokeSignature(i: Int) {
        out.print("    public fun ${if (kind.hasReceiverParameter) "T." else ""}invoke(")
        for (j in 1..i) {
            out.print("p$j: P$j")
            if (j < i) {
                out.print(", ")
            }
        }
        out.println("): R")
    }

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

        if (variance) out.print("out ")
        out.print("R>")
    }

    fun generateToStringForFunctionImpl() {
        // TODO: don't generate this in every function, create a common superclass for all functions / extension functions
        out.println("    override fun toString() = \"\${getClass().getGenericSuperclass()}\"")
    }
}

fun main(args: Array<String>) {
    for (kind in FunctionKind.values()) {
        generateRuntimeFile(kind.getFileName()) {
            GenerateFunctions(it, kind).generateFunctionClasses()
        }

        generateRuntimeFile(kind.getImplFileName()) {
            GenerateFunctions(it, kind).generateFunctionImplClasses()
        }
    }
}
