/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.fir.resolve.calls.*

fun ConeResolutionAtom.processCandidatesAndPostponedAtoms(
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit
) {
    val context = Context(candidateProcessor, postponedAtomsProcessor)
    context.processCandidatesAndPostponedAtoms(this)
}

fun ConeResolutionAtom.processPostponedAtoms(postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit) {
    processCandidatesAndPostponedAtoms(candidateProcessor = {}, postponedAtomsProcessor)
}

private class Context(
    val candidateProcessor: (Candidate) -> Unit,
    val postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit
) {
    val visited: MutableSet<ConeResolutionAtom> = mutableSetOf()
}

private fun Context.processCandidatesAndPostponedAtoms(atom: ConeResolutionAtom?) {
    if (atom == null) return
    if (!visited.add(atom)) return
    when (atom) {
        is ConeSimpleLeafResolutionAtom -> {}

        // lambdas
        is ConeResolvedLambdaAtom -> {
            postponedAtomsProcessor(atom)
            if (atom.analyzed) {
                for (returnAtom in atom.returnStatements) {
                    processCandidatesAndPostponedAtoms(returnAtom)
                }
            }
        }
        is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
            postponedAtomsProcessor(atom)
            processCandidatesAndPostponedAtoms(atom.subAtom)
        }

        // callable references
        is ConeResolvedCallableReferenceAtom -> {
            postponedAtomsProcessor(atom)
            processCandidatesAndPostponedAtoms(atom.subAtom)
        }

        // candidates
        is ConeAtomWithCandidate -> {
            val candidate = atom.candidate
            candidateProcessor(candidate)
            for (argumentAtom in candidate.arguments) {
                processCandidatesAndPostponedAtoms(argumentAtom)
            }
            for (postponedPCLAAtom in candidate.postponedPCLACalls) {
                processCandidatesAndPostponedAtoms(postponedPCLAAtom)
            }
        }

        is ConeResolutionAtomWithSingleChild -> processCandidatesAndPostponedAtoms(atom.subAtom)
        is ConeResolutionAtomWithPostponedChild -> processCandidatesAndPostponedAtoms(atom.subAtom)
    }
}
