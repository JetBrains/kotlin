/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter

import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.SuppressIntentionAction
import org.jetbrains.jet.lang.diagnostics.Severity
import java.util.Collections
import org.jetbrains.jet.plugin.quickfix.KotlinSuppressIntentionAction
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.plugin.quickfix.DeclarationKind
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import com.intellij.psi.util.PsiTreeUtil

class KotlinSuppressableWarningProblemGroup(
        private val diagnosticFactory: DiagnosticFactory
) : SuppressableProblemGroup {

    {
        assert (diagnosticFactory.getSeverity() == Severity.WARNING)
    }

    override fun getProblemName() = diagnosticFactory.getName()

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        if (element == null)
            return SuppressIntentionAction.EMPTY_ARRAY

        val actions = createSuppressWarningActions(element, diagnosticFactory)
        return actions.toArray(Array<SuppressIntentionAction?>(actions.size) {null}) as Array<SuppressIntentionAction>
    }

}

fun createSuppressWarningActions(element: PsiElement, diagnosticFactory: DiagnosticFactory): List<SuppressIntentionAction> {
    if (diagnosticFactory.getSeverity() != Severity.WARNING)
        return Collections.emptyList()

    val actions = arrayListOf<SuppressIntentionAction>()
    var current: PsiElement? = element
    while (current != null) {
        if (current is JetDeclaration) {
            val declaration = current as JetDeclaration
            val kind = DeclarationKindDetector.detect(declaration)
            if (kind != null) {
                actions.add(KotlinSuppressIntentionAction(declaration, diagnosticFactory, kind))
            }
        }

        current = current?.getParent()
    }
    return actions
}

private object DeclarationKindDetector : JetVisitor<DeclarationKind?, Unit?>() {

    fun detect(declaration: JetDeclaration) = declaration.accept(this, null)

    override fun visitDeclaration(d: JetDeclaration, _: Unit?) = null

    override fun visitClass(d: JetClass, _: Unit?) = detect(d, if (d.isTrait()) "trait" else "class")

    override fun visitClassObject(d: JetClassObject, _: Unit?) = detect(d, "class object",
                                                                        name = "of " + PsiTreeUtil.getParentOfType(d, javaClass<JetClass>())?.getName())

    override fun visitNamedFunction(d: JetNamedFunction, _: Unit?) = detect(d, "fun")

    override fun visitProperty(d: JetProperty, _: Unit?) = detect(d, d.getValOrVarNode().getText()!!)

    override fun visitMultiDeclaration(d: JetMultiDeclaration, _: Unit?) = detect(d, d.getValOrVarNode()?.getText() ?: "val",
                                                                                  name = d.getEntries().map { it.getName() }.makeString(", ", "(", ")"))

    override fun visitTypeParameter(d: JetTypeParameter, _: Unit?) = detect(d, "type parameter", newLineNeeded = false)

    override fun visitEnumEntry(d: JetEnumEntry, _: Unit?) = detect(d, "enum entry")

    override fun visitParameter(d: JetParameter, _: Unit?) = detect(d, "parameter", newLineNeeded = false)

    override fun visitObjectDeclaration(d: JetObjectDeclaration, _: Unit?): DeclarationKind? {
        if (d.getParent() is JetClassObject) return null
        return detect(d, "object")
    }

    private fun detect(declaration: JetDeclaration, kind: String, name: String = declaration.getName() ?: "<null>", newLineNeeded: Boolean = true)
        = DeclarationKind(kind, name, newLineNeeded)
}