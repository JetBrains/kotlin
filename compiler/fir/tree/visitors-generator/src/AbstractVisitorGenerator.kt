/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.utils.Printer

abstract class AbstractVisitorGenerator(val referencesData: DataCollector.ReferencesData) {
    fun Printer.generateFunction(
        name: String,
        parameters: Map<String, String>,
        returnType: String,
        override: Boolean = false,
        final: Boolean = false,
        body: (Printer.() -> Unit)?
    ) {

        if (body == null) {
            print("abstract ")
        } else {
            printIndent()
            if (!final) {
                printWithNoIndent("open ")
            }
            if (override) {
                if (final) {
                    printWithNoIndent("final ")
                }
                printWithNoIndent("override ")
            }
        }
        printWithNoIndent("fun ", name, "(")
        parameters
            .flatMap { (a, b) ->
                listOf(a, ": ", b, ", ")
            }.dropLast(1)
            .forEach {
                printWithNoIndent(it)
            }
        printWithNoIndent(")")
        if (returnType != "Unit") {
            printWithNoIndent(": ", returnType)
        }
        if (body != null) {
            printlnWithNoIndent(" {")
            indented {
                body()
            }
            println("}")
        } else {
            printlnWithNoIndent()
        }
        println()
    }

    protected inline fun Printer.indented(l: () -> Unit) {
        pushIndent()
        l()
        popIndent()
    }

    protected fun Printer.generateCall(name: String, args: List<String>) {
        printWithNoIndent(name, "(")
        separatedOneLine(args, ", ")
        printWithNoIndent(")")
    }

    protected fun Printer.separatedOneLine(iterable: Iterable<Any>, separator: Any) {
        var first = true
        for (element in iterable) {
            if (!first) {
                printWithNoIndent(separator)
            } else {
                first = false
            }
            printWithNoIndent(element)
        }
    }

    protected fun Printer.generateDefaultImports() {
        referencesData.usedPackages.forEach {
            println("import ", it.asString(), ".*")
        }
        println()
        println()
    }

    val String.safeName
        get() = when (this) {
            "class" -> "klass"
            else -> this
        }

    fun generate(): String {
        val builder = StringBuilder()
        val printer = Printer(builder, "    ")
        printer.apply {
            println(javaClass.getResource("/notice.txt").readText())

            println("package $VISITOR_PACKAGE")
            println()
            generateDefaultImports()
            println(WARNING_GENERATED_FILE)

            printer.generateContent()
        }
        return builder.toString()
    }

    abstract fun Printer.generateContent()

}