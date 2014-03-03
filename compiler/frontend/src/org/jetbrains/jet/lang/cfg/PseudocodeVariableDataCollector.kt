/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg

import org.jetbrains.jet.lang.cfg.pseudocode.Instruction
import org.jetbrains.jet.lang.cfg.pseudocode.LocalFunctionDeclarationInstruction
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.*
import org.jetbrains.jet.lang.cfg.pseudocode.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.VariableDeclarationInstruction
import org.jetbrains.jet.utils.addToStdlib.*

import kotlin.properties.Delegates

import java.util.*
import org.jetbrains.jet.lang.psi.JetDeclaration

public class PseudocodeVariableDataCollector(
        private val bindingContext: BindingContext,
        private val pseudocode: Pseudocode
) {
    val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)

    suppress("UNCHECKED_CAST")
    public fun <D> collectData(
            traversalOrder: TraversalOrder,
            mergeDataWithLocalDeclarations: Boolean,
            instructionDataMergeStrategy: InstructionDataMergeStrategy<D>
    ): MutableMap<Instruction, Edges<MutableMap<VariableDescriptor, D>>> {
        val result = pseudocode.collectData(
                traversalOrder, mergeDataWithLocalDeclarations,
                //see KT-4605
                instructionDataMergeStrategy as
                    (Instruction, Collection<Map<VariableDescriptor, D>>) -> Edges<Map<VariableDescriptor, D>>,
                { (from, to, data) -> filterOutVariablesOutOfScope(from, to, data)},
                Collections.emptyMap<VariableDescriptor, D>())
        //see KT-4605
        return result as MutableMap<Instruction, Edges<MutableMap<VariableDescriptor, D>>>
    }

    private fun <D> filterOutVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<VariableDescriptor, D>
    ): Map<VariableDescriptor, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.getLexicalScope().depth
        if (toDepth >= from.getLexicalScope().depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = lexicalScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    fun computeLexicalScopeVariableInfo(pseudocode: Pseudocode): LexicalScopeVariableInfo {
        val lexicalScopeVariableInfo = LexicalScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.getVariableDeclarationElement()
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                assert(descriptor is VariableDescriptor,
                       "Variable descriptor should correspond to the instruction for ${instruction.getElement().getText()}.\n" +
                       "Descriptor : $descriptor")
                lexicalScopeVariableInfo.registerVariableDeclaredInScope(
                        descriptor as VariableDescriptor, instruction.getLexicalScope())
            }
        })
        return lexicalScopeVariableInfo
    }
}

//todo may be a type alias
trait InstructionDataMergeStrategy<D> :
  (Instruction, Collection<MutableMap<VariableDescriptor, D>>) -> Edges<MutableMap<VariableDescriptor, D>>

public trait LexicalScopeVariableInfo {
    val declaredIn : Map<VariableDescriptor, LexicalScope>
    val scopeVariables : Map<LexicalScope, Collection<VariableDescriptor>>
}

public class LexicalScopeVariableInfoImpl : LexicalScopeVariableInfo {
    override val declaredIn = HashMap<VariableDescriptor, LexicalScope>()
    override val scopeVariables = HashMap<LexicalScope, MutableCollection<VariableDescriptor>>()

    fun registerVariableDeclaredInScope(variable: VariableDescriptor, lexicalScope: LexicalScope) {
        declaredIn[variable] = lexicalScope
        val variablesInScope = scopeVariables.getOrPut(lexicalScope, { ArrayList<VariableDescriptor>() })
        variablesInScope.add(variable)
    }
}
