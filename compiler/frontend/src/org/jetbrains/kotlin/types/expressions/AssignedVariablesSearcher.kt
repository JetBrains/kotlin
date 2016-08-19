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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

abstract class AssignedVariablesSearcher: KtTreeVisitorVoid() {

    private val assignedNames: SetMultimap<Name, KtDeclaration?> = LinkedHashMultimap.create()

    open fun writers(variableDescriptor: VariableDescriptor): MutableSet<KtDeclaration?> = assignedNames[variableDescriptor.name]

    fun hasWriters(variableDescriptor: VariableDescriptor) = writers(variableDescriptor).isNotEmpty()

    private var currentDeclaration: KtDeclaration? = null

    override fun visitDeclaration(declaration: KtDeclaration) {
        val previous = currentDeclaration
        if (declaration is KtDeclarationWithBody || declaration is KtClassOrObject) {
            currentDeclaration = declaration
        }
        else if (declaration is KtAnonymousInitializer) {
            // Go to class declaration: init -> body -> class
            currentDeclaration = declaration.parent.parent as KtDeclaration
        }
        super.visitDeclaration(declaration)
        currentDeclaration = previous
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val previous = currentDeclaration
        currentDeclaration = lambdaExpression.functionLiteral
        super.visitLambdaExpression(lambdaExpression)
        currentDeclaration = previous
    }

    override fun visitBinaryExpression(binaryExpression: KtBinaryExpression) {
        if (binaryExpression.operationToken === KtTokens.EQ) {
            val left = KtPsiUtil.deparenthesize(binaryExpression.left)
            if (left is KtNameReferenceExpression) {
                assignedNames.put(left.getReferencedNameAsName(), currentDeclaration)
            }
        }
        super.visitBinaryExpression(binaryExpression)
    }
}