/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.print

import org.jetbrains.kotlin.bir.generator.Packages
import org.jetbrains.kotlin.bir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.bir.generator.elementClassType
import org.jetbrains.kotlin.bir.generator.Model
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun printBirMetadata(generationPath: File, model: Model): GeneratedFile {
    val className = "BirMetadata"
    return printGeneratedType(generationPath, TREE_GENERATOR_README, Packages.tree, className) {
        print("object $className")
        printBlock {
            println("val allElements = listOf<${elementClassType.withArgs(TypeRef.Star).render()}>(")
            withIndent {
                model.elements
                    .forEach { element ->
                        print(element.copy(emptyMap()).render())
                        println(",")
                    }
            }
            println(")")
        }
    }
}
