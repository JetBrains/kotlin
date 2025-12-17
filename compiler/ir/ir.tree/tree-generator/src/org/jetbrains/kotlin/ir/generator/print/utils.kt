/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.nullable
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.call
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SimpleField

internal fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}

internal fun ImportCollectingPrinter.printAcceptChildrenBody(
    element: Element,
    isVoid: Boolean = false,
) {
    val callAccept = if (isVoid) "acceptVoid(visitor)" else "accept(visitor, data)"
    if (!element.isRootElement) {
        printBlock {
            for (child in element.walkableChildren) {
                print(child.name, child.call())
                when (child) {
                    is SimpleField -> println(callAccept)
                    is ListField -> {
                        print("forEach { it")
                        if (child.baseType.nullable) {
                            print("?")
                        }
                        println(".$callAccept }")
                    }
                }
            }
        }
    } else {
        println()
    }
}