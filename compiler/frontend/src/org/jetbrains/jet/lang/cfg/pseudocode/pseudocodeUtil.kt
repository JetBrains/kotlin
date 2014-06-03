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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.traverseFollowingInstructions
import java.util.HashSet
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetFunctionLiteral

fun JetExpression.isStatement(pseudocode: Pseudocode): Boolean {
    val value = pseudocode.getElementValue(this);
    if (value == null) return true

    fun considerUsedIfCreatedBeforeExit(): Boolean {
        return when {
            (getParent() as? JetFunction)?.getBodyExpression() == this ->
                true
            pseudocode.getElementValue(getParentByType(javaClass<JetFunctionLiteral>())?.getBodyExpression()) == value ->
                true
            else ->
                false
        }
    }

    val instruction = value.createdAt
    if (considerUsedIfCreatedBeforeExit() && instruction.nextInstructions.any { it == pseudocode.getExitInstruction() }) return false
    return traverseFollowingInstructions(instruction, HashSet(), TraversalOrder.FORWARD) { value !in it.inputValues }
}