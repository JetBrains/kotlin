/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.quickfix.AnnotationHostKind
import org.jetbrains.kotlin.idea.quickfix.KotlinSuppressIntentionAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.util.*

class KotlinSuppressableWarningProblemGroup(
    private val diagnosticFactory: DiagnosticFactory<*>
) : SuppressableProblemGroup {

    init {
        assert(diagnosticFactory.severity == Severity.WARNING)
    }

    override fun getProblemName() = diagnosticFactory.name

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        if (element == null)
            return SuppressIntentionAction.EMPTY_ARRAY

        return createSuppressWarningActions(element, diagnosticFactory).toTypedArray()
    }

}

fun createSuppressWarningActions(element: PsiElement, diagnosticFactory: DiagnosticFactory<*>): List<SuppressIntentionAction> =
    createSuppressWarningActions(element, diagnosticFactory.severity, diagnosticFactory.name!!)


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
                    val kind = if (current.parent is KtBlockExpression)
                        KotlinIdeaAnalysisBundle.message("declaration.kind.statement")
                    else
                        KotlinIdeaAnalysisBundle.message("declaration.kind.initializer")

                    actions.add(
                        KotlinSuppressIntentionAction(
                            current,
                            suppressionKey,
                            AnnotationHostKind(kind, "", true)
                        )
                    )
                    suppressAtStatementAllowed = false
                }
            }

            current is KtFile -> {
                actions.add(
                    KotlinSuppressIntentionAction(
                        current,
                        suppressionKey,
                        AnnotationHostKind(
                            KotlinIdeaAnalysisBundle.message("declaration.kind.file"),
                            current.name,
                            true
                        )
                    )
                )
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

    override fun visitClass(d: KtClass, data: Unit?) = detect(
        d,
        if (d.isInterface())
            KotlinIdeaAnalysisBundle.message("declaration.kind.interface")
        else
            KotlinIdeaAnalysisBundle.message("declaration.kind.class")
    )

    override fun visitNamedFunction(d: KtNamedFunction, data: Unit?) = detect(d, KotlinIdeaAnalysisBundle.message("declaration.kind.fun"))

    override fun visitProperty(d: KtProperty, data: Unit?) = detect(d, d.valOrVarKeyword.text!!)

    override fun visitDestructuringDeclaration(d: KtDestructuringDeclaration, data: Unit?) =
        detect(d, d.valOrVarKeyword?.text ?: "val", name = d.entries.joinToString(", ", "(", ")") { it.name!! })

    override fun visitTypeParameter(d: KtTypeParameter, data: Unit?) = detect(
        d,
        KotlinIdeaAnalysisBundle.message("declaration.kind.type.parameter"), newLineNeeded = false
    )

    override fun visitEnumEntry(d: KtEnumEntry, data: Unit?) = detect(d, KotlinIdeaAnalysisBundle.message("declaration.kind.enum.entry"))

    override fun visitParameter(d: KtParameter, data: Unit?) = detect(
        d,
        KotlinIdeaAnalysisBundle.message("declaration.kind.parameter"),
        newLineNeeded = false
    )

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?) =
        detect(constructor, KotlinIdeaAnalysisBundle.message("declaration.kind.secondary.constructor.of"))

    override fun visitObjectDeclaration(d: KtObjectDeclaration, data: Unit?): AnnotationHostKind? {
        if (d.isCompanion()) return detect(
            d,
            KotlinIdeaAnalysisBundle.message("declaration.kind.companion.object"),
            name = KotlinIdeaAnalysisBundle.message(
                "declaration.name.0.of.1",
                d.name.toString(),
                d.getStrictParentOfType<KtClass>()?.name.toString()
            )
        )

        if (d.parent is KtObjectLiteralExpression) return null
        return detect(d, KotlinIdeaAnalysisBundle.message("declaration.kind.object"))
    }

    private fun detect(
        declaration: KtDeclaration,
        kind: String,
        name: String = declaration.name ?: "<anonymous>",
        newLineNeeded: Boolean = true
    ) = AnnotationHostKind(kind, name, newLineNeeded)
}
