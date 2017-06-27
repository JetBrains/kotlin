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

import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.quickfix.AnnotationHostKind
import org.jetbrains.kotlin.idea.quickfix.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.util.*

class KotlinSuppressableWarningProblemGroup(
        private val diagnosticFactory: DiagnosticFactory<*>
) : SuppressableProblemGroup {

    init {
        assert (diagnosticFactory.severity == Severity.WARNING)
    }

    override fun getProblemName() = diagnosticFactory.name

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        if (element == null)
            return SuppressIntentionAction.EMPTY_ARRAY

        return createSuppressWarningActions(element, diagnosticFactory).toTypedArray()
    }

}

fun createSuppressWarningActions(element: PsiElement, diagnosticFactory: DiagnosticFactory<*>): List<SuppressIntentionAction> =
        createSuppressWarningActions(element, diagnosticFactory.severity, diagnosticFactory.name)


fun createSuppressWarningActions(element: PsiElement, severity: Severity, suppressionKey: String): List<SuppressIntentionAction> {
    if (severity != Severity.WARNING) return Collections.emptyList()

    val actions = arrayListOf<SuppressIntentionAction>()
    var current: PsiElement? = element
    var suppressAtStatementAllowed = true
    while (current != null) {
        when {
            current is KtDeclaration && current !is KtDestructuringDeclaration -> {
                val declaration = current
                val kind = DeclarationKindDetector.detect(declaration)
                if (kind != null) {
                    actions.add(KotlinSuppressIntentionAction(declaration, suppressionKey, kind))
                }
                suppressAtStatementAllowed = false
            }

            current is KtExpression && suppressAtStatementAllowed -> {
                // Add suppress action at first statement
                if (current.parent is KtBlockExpression || current.parent is KtDestructuringDeclaration) {
                    val kind = if (current.parent is KtBlockExpression) "statement" else "initializer"
                    actions.add(KotlinSuppressIntentionAction(current, suppressionKey,
                                                              AnnotationHostKind(kind, "", true)))
                    suppressAtStatementAllowed = false
                }
            }

            current is KtFile -> {
                actions.add(KotlinSuppressIntentionAction(current, suppressionKey,
                                                          AnnotationHostKind("file", current.name, true)))
                suppressAtStatementAllowed = false
            }
        }

        current = current.parent
    }
    return actions
}

private object DeclarationKindDetector : KtVisitor<AnnotationHostKind?, Unit?>() {

    fun detect(declaration: KtDeclaration) = declaration.accept(this, null)

    override fun visitDeclaration(d: KtDeclaration, data: Unit?) = null

    override fun visitClass(d: KtClass, data: Unit?) = detect(d, if (d.isInterface()) "interface" else "class")

    override fun visitNamedFunction(d: KtNamedFunction, data: Unit?) = detect(d, "fun")

    override fun visitProperty(d: KtProperty, data: Unit?) = detect(d, d.valOrVarKeyword.text!!)

    override fun visitDestructuringDeclaration(d: KtDestructuringDeclaration, data: Unit?) = detect(d, d.valOrVarKeyword?.text ?: "val",
                                                                                                    name = d.entries.joinToString(", ", "(", ")") { it.name!! })

    override fun visitTypeParameter(d: KtTypeParameter, data: Unit?) = detect(d, "type parameter", newLineNeeded = false)

    override fun visitEnumEntry(d: KtEnumEntry, data: Unit?) = detect(d, "enum entry")

    override fun visitParameter(d: KtParameter, data: Unit?) = detect(d, "parameter", newLineNeeded = false)

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?) = detect(constructor, "secondary constructor of")

    override fun visitObjectDeclaration(d: KtObjectDeclaration, data: Unit?): AnnotationHostKind? {
        if (d.isCompanion()) return detect(d, "companion object", name = "${d.name} of ${d.getStrictParentOfType<KtClass>()?.name}")
        if (d.parent is KtObjectLiteralExpression) return null
        return detect(d, "object")
    }

    private fun detect(declaration: KtDeclaration, kind: String, name: String = declaration.name ?: "<anonymous>", newLineNeeded: Boolean = true)
        = AnnotationHostKind(kind, name, newLineNeeded)
}
