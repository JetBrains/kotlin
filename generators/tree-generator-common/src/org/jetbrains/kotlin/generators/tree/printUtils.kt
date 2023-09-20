/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }

class GeneratedFile(val file: File, val newText: String)

fun getPathForFile(generationPath: File, packageName: String, typeName: String): File {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    return File(dir, "$typeName.kt")
}

fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

fun SmartPrinter.printKDoc(kDoc: String?) {
    if (kDoc == null) return
    println("/**")
    for (line in kDoc.lineSequence()) {
        print(" *")
        if (line.isBlank()) {
            println()
        } else {
            print(" ")
            println(line)
        }
    }
    println(" */")
}

fun SmartPrinter.printFunctionDeclaration(
    name: String,
    parameters: List<Pair<String, TypeRef>>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    modality: Modality = Modality.FINAL,
    override: Boolean = false,
) {
    when (modality) {
        Modality.FINAL -> if (override) {
            print("final ")
        }
        Modality.OPEN -> if (!override) {
            print("open ")
        }
        Modality.ABSTRACT -> print("abstract ")
        Modality.SEALED -> error("Function cannot be sealed")
    }
    if (override) {
        print("override ")
    }
    print("fun ")
    if (typeParameters.isNotEmpty()) {
        print(typeParameters.generics)
        print(" ")
    }
    print(name)
    print(
        parameters.joinToString(prefix = "(", postfix = ")") { (name, type) ->
            "$name: ${type.typeWithArguments}"
        }
    )
    if (returnType != StandardTypes.unit) {
        print(": ", returnType.typeWithArguments)
    }
}