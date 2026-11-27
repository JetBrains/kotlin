/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.irTransformerType
import org.jetbrains.kotlin.ir.generator.model.Element

internal fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}

/** The file annotation opting in to the contracts printed by [printCallsInPlaceContract]. */
internal const val CONTRACTS_OPT_IN = "OptIn(ExperimentalContracts::class)"

/**
 * Prints the contract of an entry point calling its builder lambda in place exactly once, so that the lambda may initialize
 * `val`s declared outside of it (e.g. a parameter symbol). The file must have the [CONTRACTS_OPT_IN] annotation.
 */
internal fun ImportCollectingPrinter.printCallsInPlaceContract(lambdaName: String) {
    addImport(ArbitraryImportable("kotlin.contracts", "ExperimentalContracts"))
    addImport(ArbitraryImportable("kotlin.contracts", "InvocationKind"))
    addImport(ArbitraryImportable("kotlin.contracts", "contract"))
    println("contract { callsInPlace(", lambdaName, ", InvocationKind.EXACTLY_ONCE) }")
}
