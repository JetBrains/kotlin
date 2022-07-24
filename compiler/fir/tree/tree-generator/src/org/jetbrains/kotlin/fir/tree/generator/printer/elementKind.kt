/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import java.io.File
import org.jetbrains.kotlin.util.SmartPrinter


fun printElementKind(elements: List<Element>, generationPath: File): GeneratedFile {
    val className = "FirElementKind"
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "$className.kt")
    val stringBuilder = StringBuilder()

    val elementsWithChildren = mutableSetOf<Element>()
    for (element in elements) {
        element.parents.forEach { elementsWithChildren.add(it) }
    }

    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        printGeneratedMessage()
        println("enum class $className {")
        pushIndent()
        for (element in elements) {
            if (element in elementsWithChildren && element.allImplementations.isEmpty() && !element.hasManualImplementations) continue
            println("${element.name},")
        }
        popIndent()
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}