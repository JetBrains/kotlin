/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractVisitorPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.ir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

internal fun printVisitorCommon(
    generationPath: File,
    model: Model,
    visitorType: ClassRef<*>,
    makePrinter: (SmartPrinter, ClassRef<*>) -> AbstractVisitorPrinter<Element, Field>,
): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, visitorType.packageName, visitorType.simpleName) {
        makePrinter(this, visitorType).printVisitor(model.elements)
    }

internal fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}
