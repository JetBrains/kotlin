/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

/**
 * All [AbiFunction]s in this container and, recursively, in the containers nested in it. Property getters and setters
 * are included, as they are [AbiFunction]s too but are not [declarations][AbiDeclarationContainer.declarations]
 * of the container themselves.
 */
@ExperimentalLibraryAbiReader
fun AbiDeclarationContainer.allFunctions(): List<AbiFunction> = buildList { collectFunctionsTo(this) }

@OptIn(ExperimentalLibraryAbiReader::class)
private fun AbiDeclarationContainer.collectFunctionsTo(destination: MutableList<AbiFunction>) {
    for (declaration in declarations) {
        when (declaration) {
            is AbiFunction -> destination += declaration
            is AbiProperty -> {
                declaration.getter?.let { destination += it }
                declaration.setter?.let { destination += it }
            }
            else -> {}
        }
        if (declaration is AbiDeclarationContainer) declaration.collectFunctionsTo(destination)
    }
}
