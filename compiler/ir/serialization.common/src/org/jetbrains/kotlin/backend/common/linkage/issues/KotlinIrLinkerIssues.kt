/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

abstract class KotlinIrLinkerIssue {
    protected abstract val errorMessage: String

    fun raiseIssue(consumer: (String) -> Unit): Nothing {
        consumer(errorMessage)
        throw CompilationErrorException(errorMessage)
    }
}

object PartialLinkageErrorsLogged : KotlinIrLinkerIssue() {
    override val errorMessage = "There are linkage errors reported by the partial linkage engine"

    fun raiseIssue(diagnosticReporter: IrDiagnosticReporter): Nothing {
        diagnosticReporter.report(PartialLinkageDiagnostics.MAJOR_PARTIAL_LINKAGE_ISSUE, errorMessage)
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

class SignatureIdNotFoundInModuleWithDependencies(
    private val idSignature: IdSignature,
    private val problemModuleDeserializer: IrModuleDeserializer,
) : KotlinIrLinkerIssue() {
    override val errorMessage = try {
        computeErrorMessage()
    } catch (_: Throwable) {
        // Don't suppress the real cause if computation of error message failed.
        throw RuntimeException(
            buildString {
                appendLine("Failed to compute the detailed error message. See the root cause exception.")
                appendLine()
                append("Shortly: The required symbol ${idSignature.render()} is missing in the module or module dependencies.")
                append(" This could happen if the required dependency is missing in the project.")
                append(" Or if there is a dependency that has a different version (without the required symbol) in the project")
                append(" than the version (with the required symbol) that the module was initially compiled with.")
            }
        )
    }

    private fun computeErrorMessage() = buildString {
        val problemModuleId = problemModuleDeserializer.moduleFragment.name.asStringStripSpecialMarkers()

        // cause:
        append("Module \"$problemModuleId\" has a reference to symbol ${idSignature.render()}.")
        append(" Neither the module itself nor its dependencies contain such declaration.")

        val fromCInteropLibrary = with(idSignature) { IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test() }
        if (fromCInteropLibrary) {
            val packageFqName = idSignature.packageFqName().asString()
            val platformLibraryName = packageFqName.removePrefix("org.jetbrains.kotlin.native.platform.")
            if (platformLibraryName != packageFqName) {
                // explanation:
                append("\n\nThis looks like a Kotlin/Native $platformLibraryName platform library issue. It could happen if")
                append(" \"$problemModuleId\" was compiled with a different version of the Kotlin/Native compiler")
                append(" than the version currently used in the project.")

                // action items:
                append(" Please check that the project configuration is correct and has consistent versions of all required dependencies.")
                append(" See https://youtrack.jetbrains.com/issue/KT-78063 for more details.")
            } else { // A user cinterop library.
                // explanation:
                append("\n\nThis looks like a cinterop-generated library issue. It could happen if there is a transitive dependency")
                append(" of \"$problemModuleId\" which uses cinterop and the resulting libraries are not binary compatible.")
                append(" Or there might be a cinterop dependency of \\\"$problemModuleId\\\" generated by a different version of")
                append(" the Kotlin/Native compiler than the version that \\\"$problemModuleId\\\" was initially compiled with.")

                // action items:
                append(" Please check that the project configuration is correct and has consistent versions of all required dependencies.")
                append(" See https://youtrack.jetbrains.com/issue/KT-78062 for more details.")
            }
        } else {
            // explanation:
            append("\n\nThis could happen if the required dependency is missing in the project.")
            append(" Or if there is a dependency of \"$problemModuleId\" that has a different version in the project")
            append(" than the version that \"$problemModuleId\" was initially compiled with.")

            // action items:
            append(" Please check that the project configuration is correct and has consistent versions of all required dependencies.")
        }
    }
}

class SymbolTypeMismatch(
    private val cause: IrSymbolTypeMismatchException,
) : KotlinIrLinkerIssue() {
    override val errorMessage = try {
        computeErrorMessage()
    } catch (e: Throwable) {
        // Don't suppress the real cause if computation of error message failed.
        throw if (e === cause) e else e.apply { addSuppressed(cause) }
    }

    private fun computeErrorMessage() = buildString {
        // cause:
        append(cause.message)

        // explanation:
        append("\n\nThis could happen if there are two libraries, where one library was compiled against the different version")
        append(" of the other library than the one currently used in the project.")

        // action items:
        append(" Please check that the project configuration is correct and has consistent versions of dependencies.")
    }
}
