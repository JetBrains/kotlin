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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.SuppressIntentionAction
import org.jetbrains.kotlin.diagnostics.Severity
import java.util.Collections
import org.jetbrains.kotlin.idea.quickfix.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.quickfix.AnnotationHostKind
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinSuppressableWarningProblemGroup(
        private val diagnosticFactory: DiagnosticFactory<*>
) : SuppressableProblemGroup {

    {
        assert (diagnosticFactory.getSeverity() == Severity.WARNING)
    }

    override fun getProblemName() = diagnosticFactory.getName()

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        if (element == null)
            return SuppressIntentionAction.EMPTY_ARRAY

        return createSuppressWarningActions(element, diagnosticFactory).copyToArray()
    }

}

fun createSuppressWarningActions(element: PsiElement, diagnosticFactory: DiagnosticFactory<*>): List<SuppressIntentionAction> {
    if (diagnosticFactory.getSeverity() != Severity.WARNING)
        return Collections.emptyList()

    val actions = arrayListOf<SuppressIntentionAction>()
    var current: PsiElement? = element
    var suppressAtStatementAllowed = true
    while (current != null) {
        if (current is JetDeclaration) {
            val declaration = current as JetDeclaration
            val kind = DeclarationKindDetector.detect(declaration)
            if (kind != null) {
                actions.add(KotlinSuppressIntentionAction(declaration, diagnosticFactory, kind))
            }
            suppressAtStatementAllowed = false
        }
        else if (current is JetExpression && suppressAtStatementAllowed) {
            // Add suppress action at first statement
            if ((current as PsiElement).getParent() is JetBlockExpression) {
                val expression = current as JetExpression
                actions.add(KotlinSuppressIntentionAction(expression, diagnosticFactory,
                                                          AnnotationHostKind("statement", "", true)))
                suppressAtStatementAllowed = false
            }
        }

        current = current?.getParent()
    }
    return actions
}

private object DeclarationKindDetector : JetVisitor<AnnotationHostKind?, Unit?>() {

    fun detect(declaration: JetDeclaration) = declaration.accept(this, null)

    override fun visitDeclaration(d: JetDeclaration, _: Unit?) = null

    override fun visitClass(d: JetClass, _: Unit?) = detect(d, if (d.isTrait()) "trait" else "class")

    override fun visitNamedFunction(d: JetNamedFunction, _: Unit?) = detect(d, "fun")

    override fun visitProperty(d: JetProperty, _: Unit?) = detect(d, d.getValOrVarNode().getText()!!)

    override fun visitMultiDeclaration(d: JetMultiDeclaration, _: Unit?) = detect(d, d.getValOrVarNode()?.getText() ?: "val",
                                                                                  name = d.getEntries().map { it.getName() }.join(", ", "(", ")"))

    override fun visitTypeParameter(d: JetTypeParameter, _: Unit?) = detect(d, "type parameter", newLineNeeded = false)

    override fun visitEnumEntry(d: JetEnumEntry, _: Unit?) = detect(d, "enum entry")

    override fun visitParameter(d: JetParameter, _: Unit?) = detect(d, "parameter", newLineNeeded = false)

    override fun visitObjectDeclaration(d: JetObjectDeclaration, _: Unit?): AnnotationHostKind? {
        if (d.isDefault()) return detect(d, "default object", name = "${d.getName()} of ${d.getStrictParentOfType<JetClass>()?.getName()}")
        if (d.getParent() is JetObjectLiteralExpression) return null
        return detect(d, "object")
    }

    private fun detect(declaration: JetDeclaration, kind: String, name: String = declaration.getName() ?: "<anonymous>", newLineNeeded: Boolean = true)
        = AnnotationHostKind(kind, name, newLineNeeded)
}
