/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetNameReferenceExpression
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetTreeVisitorVoid
import java.util.*

abstract class AssignedVariablesSearcher: JetTreeVisitorVoid() {

    protected val assignedNames: MutableSet<Name> = LinkedHashSet()

    override fun visitBinaryExpression(binaryExpression: JetBinaryExpression) {
        if (binaryExpression.operationToken === JetTokens.EQ) {
            val left = JetPsiUtil.deparenthesize(binaryExpression.left)
            if (left is JetNameReferenceExpression) {
                assignedNames += left.getReferencedNameAsName()
            }
        }
    }

}