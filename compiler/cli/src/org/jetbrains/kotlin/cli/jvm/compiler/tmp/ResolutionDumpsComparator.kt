/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.RDumpErrorCall
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.RDumpResolvedCall
import org.jetbrains.kotlin.utils.addIfNotNull

class ResolutionDumpsComparator {
    fun compareDumpsForFile(before: RDumpForFile, after: RDumpForFile) {
        check(before.ownerFile == after.ownerFile) { "Trying to compare dumps for different files" }

        val offsetToElementMapBefore = before.results.associateBy { it.offset }
        val offsetToElementMapAfter = after.results.associateBy { it.offset }

        val allOffsets = offsetToElementMapBefore.keys union offsetToElementMapAfter.keys

        val diff: MutableList<ResolutionDifference> = mutableListOf()
        for (offset in allOffsets) {
            val elementBefore = offsetToElementMapBefore[offset]
            val elementAfter = offsetToElementMapAfter[offset]

            val difference = when {
                elementBefore != null && elementAfter == null ->
                    ResolutionDifference(elementBefore.presentableText, MissingInDump(elementBefore, elementAfter))

                elementBefore == null && elementAfter != null ->
                    ResolutionDifference(elementAfter.presentableText, MissingInDump(elementBefore, elementAfter))

                elementBefore != null && elementAfter != null -> compareResolutionElements(elementBefore, elementAfter)

                else -> throw IllegalStateException("No elements at offset $offset")
            }

            diff.addIfNotNull(difference)
        }
    }

    private fun compareResolutionElements(before: RDumpElement, after: RDumpElement): ResolutionDifference? {
        check(before.presentableText == after.presentableText) {
            "Elements at the same offsets in the same file have different presentable texts:\n" +
                    "   First: $before\n" +
                    "   Second: $after"
        }

        if (before::class != after::class) {
            return ResolutionDifference(before.presentableText, ResolutionStatusMismatch(before, after))
        }

        return when {
            before is RDumpExpression && after is RDumpExpression -> compareExpressions(before, after)
            else -> throw IllegalStateException("Unknown Element type: $before")
        }
    }

    private fun compareExpressions(before: RDumpExpression, after: RDumpExpression): ResolutionDifference? {
        return when {
            before is RDumpResolvedCall && after is RDumpResolvedCall -> compareResolved(before, after)
            before is RDumpErrorCall && after is RDumpErrorCall -> compareErrorCalls(before, after)
            else -> throw IllegalStateException("Unknown Expression type: $before")
        }
    }

    private fun compareErrorCalls(before: RDumpErrorCall, after: RDumpErrorCall): ResolutionDifference? {
        val diagnosticsSetBefore = before.diagnostics.toSet()
        val diagnosticsSetAfter = after.diagnostics.toSet()

        val allDiagnostics = diagnosticsSetBefore union diagnosticsSetAfter

        val mismatchedDiagnostics: MutableList<DiagnosticsMismatch.MismatchedDiagnostic> = mutableListOf()
        iterateOverDiagnostics@ for (diagnostic in allDiagnostics) {
            when {
                diagnostic in diagnosticsSetBefore && diagnostic in diagnosticsSetAfter ->
                    continue@iterateOverDiagnostics

                diagnostic in diagnosticsSetBefore && diagnostic !in diagnosticsSetAfter ->
                    mismatchedDiagnostics += DiagnosticsMismatch.LostDiagnostic(diagnostic)

                diagnostic !in diagnosticsSetBefore && diagnostic in diagnosticsSetAfter ->
                    mismatchedDiagnostics += DiagnosticsMismatch.NewDiagnostic(diagnostic)

                else -> throw IllegalStateException()
            }
        }

        return if (mismatchedDiagnostics.isNotEmpty())
            ResolutionDifference(before.presentableText, DiagnosticsMismatch(mismatchedDiagnostics))
        else
            null
    }

    private fun compareResolved(before: RDumpResolvedCall, after: RDumpResolvedCall): ResolutionDifference? {
        if (before.candidateDescriptor != after.candidateDescriptor) {
            return ResolutionDifference(
                before.presentableText,
                CandidateDescriptorMismatch(before.candidateDescriptor, after.candidateDescriptor)
            )
        }

        val typeParameterDifferences: MutableList<InferenceMismatch.TypeParameterInference> = mutableListOf()
        for (i in before.candidateDescriptor.typeParameters.indices) {
            val typeParameterBefore = before.candidateDescriptor.typeParameters[i]
            val typeParameterAfter = after.candidateDescriptor.typeParameters[i]
            check(typeParameterBefore == typeParameterAfter) {
                "Type parameters in candidate descriptors are not the same: for index $i before was $typeParameterBefore, after became $typeParameterAfter"
            }

            val typeArgumentBefore = before.typeArguments[i]
            val typeArgumentAfter = after.typeArguments[i]

            if (typeArgumentBefore != typeArgumentAfter) {
                typeParameterDifferences.add(
                    InferenceMismatch.TypeParameterInference(
                        typeParameterBefore,
                        typeArgumentBefore,
                        typeArgumentAfter
                    )
                )
            }
        }

        return if (typeParameterDifferences.isNotEmpty())
            ResolutionDifference(before.presentableText, InferenceMismatch(typeParameterDifferences))
        else
            null
    }

}


class ResolutionDifference(val elementText: String, val reason: MismatchReason) {
    fun render(): String =
        "Resolution mismatch for $elementText\n" + reason.reasonText
}


sealed class MismatchReason(val reasonText: String)

class MissingInDump(before: RDumpElement?, after: RDumpElement?) :
    MismatchReason(
        "Missing resolution result in one of the dumps:\n" +
                "    Was: ${before ?: "<ABSENT>"}\n" +
                "    Become: ${after ?: "<ABSENT>"}\n"
    )

class CandidateDescriptorMismatch(beforeDescriptor: RDumpDescriptor, afterDescriptor: RDumpDescriptor) :
    MismatchReason(
        "Candidate descriptor have changed:\n" +
                "   Was: $beforeDescriptor\n" +
                "   Become: $afterDescriptor"
    )

class InferenceMismatch(val mismatchedTypeParameters: List<TypeParameterInference>) :
    MismatchReason(
        "Inference for type parameter(s) have changed:\n" +
                mismatchedTypeParameters.joinToString(separator = "\n\n")
    ) {

    data class TypeParameterInference(val typeParameterDescriptor: RDumpDescriptor, val before: RDumpType, val after: RDumpType) {
        override fun toString(): String = "Type parameter: $typeParameterDescriptor\n" +
                "Was: $before\n" +
                "Become: $after"
    }
}


// E.g. Resolved -> Error
class ResolutionStatusMismatch(before: RDumpElement, after: RDumpElement) :
    MismatchReason(
        "Resolution kind have changed:" +
                "   Was: $before\n" +
                "   After: $after"
    )

class DiagnosticsMismatch(mismatchedDiagnostics: List<MismatchedDiagnostic>) :
    MismatchReason("Diagnostics are different:\n" + mismatchedDiagnostics.joinToString(separator = "\n")) {

    interface MismatchedDiagnostic

    class NewDiagnostic(val diagnostic: RDumpDiagnostic) : MismatchedDiagnostic {
        override fun toString(): String = "Appeared new diagnostic: \"$diagnostic\""
    }

    class LostDiagnostic(val diagnostic: RDumpDiagnostic) : MismatchedDiagnostic {
        override fun toString(): String = "Missing diagnostic: \"$diagnostic\""
    }
}



