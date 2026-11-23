/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class KotlinIrLinkerIssue {
    protected abstract val errorMessage: String

    fun raiseIssue(consumer: (String) -> Unit): Nothing {
        consumer(errorMessage)
        throw CompilationErrorException(errorMessage)
    }
}

class UnexpectedUnboundIrSymbols(unboundSymbols: Set<IrSymbol>, whenDetected: String) : KotlinIrLinkerIssue() {
    override val errorMessage = buildString {
        // cause:
        append("There ").append(
            when (val count = unboundSymbols.size) {
                1 -> "is still an unbound symbol"
                else -> "are still $count unbound symbols"
            }
        ).append(" ").append(whenDetected).append(":\n")
        unboundSymbols.joinTo(this, separator = "\n")

        // explanation:
        append("\n\nThis could happen if there are two libraries, where one library was compiled against the different version")
        append(" of the other library than the one currently used in the project.")

        // action items:
        append(" Please check that the project configuration is correct and has consistent versions of dependencies.")
        if (unboundSymbols.any { looksLikeEnumEntries(it.signature) }) {
            append("\n\nAnother possible reason is that some parts of the project are compiled with EnumEntries language feature enabled,")
            append(" but other parts or used libraries are compiled with EnumEntries language feature disabled.")
        }
    }

    companion object {
        fun looksLikeEnumEntries(signature: IdSignature?): Boolean = when (signature) {
            is IdSignature.AccessorSignature -> looksLikeEnumEntries(signature.propertySignature)
            is IdSignature.CompositeSignature -> looksLikeEnumEntries(signature.inner)
            is IdSignature.CommonSignature -> signature.shortName == "entries"
            else -> false
        }
    }
}
