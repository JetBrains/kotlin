/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import java.io.File

object PCLALog {

    val file = File("output/pcla.txt")

    fun log(str: String) {
//        file.appendText(str + "\n")
    }

    fun logStorage(system: ConstraintStorage) {
        log(
            buildString {
                system.outerSystemVariablesPrefixSize
                appendLine("Constraint system: $system")
                if (system.usesOuterCs) {
                    appendLine(
                        "outerTypeVariables = ${
                            system.notFixedTypeVariables.keys.take(system.outerSystemVariablesPrefixSize)
                                .joinToString() { (it as ConeTypeVariableTypeConstructor).debugName }
                        }"
                    )
                }
                appendLine(
                    "notFixed = ${system.notFixedTypeVariables.keys.joinToString() { (it as ConeTypeVariableTypeConstructor).debugName }}"
                )
                appendLine(
                    "fixed = ${system.fixedTypeVariables.entries.joinToString { (it.key as ConeTypeVariableTypeConstructor).debugName + " -> " + (it.value as ConeKotlinType).renderForDebugging() }}"
                )
                appendLine(
                    "initialConstraints = ["
                )
                system.initialConstraints.forEach {
                    appendLine("    ${it.asStringWithoutPosition()}")
                }
                appendLine("]")
            }
        )
    }
}