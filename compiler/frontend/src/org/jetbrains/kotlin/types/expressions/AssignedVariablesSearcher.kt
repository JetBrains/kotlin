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

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

abstract class AssignedVariablesSearcher: JetTreeVisitorVoid() {

    private val assignedNames: SetMultimap<Name, JetDeclaration?> = LinkedHashMultimap.create()

    public open fun writers(variableDescriptor: VariableDescriptor) = assignedNames[variableDescriptor.name]

    public fun hasWriters(variableDescriptor: VariableDescriptor) = writers(variableDescriptor).isNotEmpty()

    private var currentDeclaration: JetDeclaration? = null

    override fun visitDeclaration(declaration: JetDeclaration) {
        val previous = currentDeclaration
        if (declaration is JetDeclarationWithBody || declaration is JetClassOrObject) {
            currentDeclaration = declaration
        }
        else if (declaration is JetClassInitializer) {
            // Go to class declaration: init -> body -> class
            currentDeclaration = declaration.parent.parent as JetDeclaration
        }
        super.visitDeclaration(declaration)
        currentDeclaration = previous
    }

    override fun visitFunctionLiteralExpression(functionLiteralExpression: JetFunctionLiteralExpression) {
        val previous = currentDeclaration
        currentDeclaration = functionLiteralExpression.functionLiteral
        super.visitFunctionLiteralExpression(functionLiteralExpression)
        currentDeclaration = previous
    }

    override fun visitBinaryExpression(binaryExpression: JetBinaryExpression) {
        if (binaryExpression.operationToken === JetTokens.EQ) {
            val left = JetPsiUtil.deparenthesize(binaryExpression.left)
            if (left is JetNameReferenceExpression) {
                assignedNames.put(left.getReferencedNameAsName(), currentDeclaration)
            }
        }
        super.visitBinaryExpression(binaryExpression)
    }
}