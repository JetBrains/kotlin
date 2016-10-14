/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableMutabilityFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class CanBeValInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            private val pseudocodeCache = HashMap<KtDeclaration, Pseudocode>()

            override fun visitDeclaration(declaration: KtDeclaration) {
                super.visitDeclaration(declaration)

                when (declaration) {
                    is KtProperty -> {
                        if (declaration.isVar && declaration.isLocal && canBeVal(declaration, declaration.hasInitializer(), listOf(declaration))) {
                            reportCanBeVal(declaration)
                        }
                    }

                    is KtDestructuringDeclaration -> {
                        val entries = declaration.entries
                        if (declaration.isVar && entries.all { canBeVal(it, true, entries) }) {
                            reportCanBeVal(declaration)
                        }
                    }
                }
            }

            private fun canBeVal(declaration: KtVariableDeclaration, hasInitializer: Boolean, allDeclarations: Collection<KtVariableDeclaration>): Boolean {
                if (allDeclarations.all { ReferencesSearch.search(it, it.useScope).none() }) {
                    // do not report for unused var's (otherwise we'll get it highlighted immediately after typing the declaration
                    return false
                }

                if (hasInitializer) {
                    val hasWriteUsages = ReferencesSearch.search(declaration, declaration.useScope).any {
                        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
                    }
                    return !hasWriteUsages
                }
                else {
                    val bindingContext = declaration.analyze(BodyResolveMode.FULL)
                    val pseudocode = pseudocode(declaration, bindingContext) ?: return false
                    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return false

                    val writeInstructions = pseudocode.collectWriteInstructions(descriptor)
                    if (writeInstructions.isEmpty()) return false // incorrect code - do not report

                    return writeInstructions.none { canReach(it, writeInstructions) }
                }
            }

            private fun pseudocode(element: KtElement, bindingContext: BindingContext): Pseudocode? {
                val declaration = element.containingDeclarationForPseudocode ?: return null
                return pseudocodeCache.getOrPut(declaration) { PseudocodeUtil.generatePseudocode(declaration, bindingContext) }
            }

            private fun Pseudocode.collectWriteInstructions(descriptor: DeclarationDescriptor): Set<WriteValueInstruction> =
                    with (instructionsIncludingDeadCode) {
                        filterIsInstance<WriteValueInstruction>()
                        .filter { (it.target as? AccessTarget.Call)?.resolvedCall?.resultingDescriptor == descriptor }
                        .toSet() +

                        filterIsInstance<LocalFunctionDeclarationInstruction>()
                        .map { it.body.collectWriteInstructions(descriptor) }
                        .flatten()
                    }

            private fun canReach(from: Instruction, targets: Set<Instruction>, visited: HashSet<Instruction> = HashSet<Instruction>()): Boolean {
                // special algorithm for linear code to avoid too deep recursion
                var instruction = from
                while (instruction is InstructionWithNext) {
                    if (instruction is LocalFunctionDeclarationInstruction) {
                        if (canReach(instruction.body.enterInstruction, targets, visited)) return true
                    }
                    val next = instruction.next ?: return false
                    if (next in visited) return false
                    if (next in targets) return true
                    visited.add(next)
                    instruction = next
                }

                for (next in instruction.nextInstructions) {
                    if (next in visited) continue
                    if (next in targets) return true
                    visited.add(next)
                    if (canReach(next, targets, visited)) return true
                }
                return false
            }

            private fun reportCanBeVal(declaration: KtValVarKeywordOwner) {
                val keyword = declaration.valOrVarKeyword!!
                val problemDescriptor = holder.manager.createProblemDescriptor(
                        keyword,
                        keyword,
                        "Variable is never modified and can be declared immutable using 'val'",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOnTheFly,
                        IntentionWrapper(ChangeVariableMutabilityFix(declaration, false), declaration.containingFile)
                )
                holder.registerProblem(problemDescriptor)
            }
        }
    }
}