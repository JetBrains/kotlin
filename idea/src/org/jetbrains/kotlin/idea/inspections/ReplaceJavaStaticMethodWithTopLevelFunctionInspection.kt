/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceJavaStaticMethodWithTopLevelFunctionInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val callee = call.calleeExpression ?: return
        val dotQualified = call.getStrictParentOfType<KtDotQualifiedExpression>() ?: return
        val calleeText = callee.text
        val replacements = REPLACEMENTS[calleeText]?.let { list ->
            val callDescriptor = call.getResolvedCall(call.analyze(BodyResolveMode.PARTIAL)) ?: return
            list.filter { callDescriptor.isCalling(FqName(it.javaMethodFqName)) }
        } ?: return

        val replacement = replacements.firstOrNull() ?: return
        if (replacement.toExtensionFunction && call.valueArguments.isEmpty()) return
        holder.registerProblem(
            dotQualified,
            TextRange(0, callee.endOffset - dotQualified.startOffset),
            "Should be replaced with '${replacement.kotlinFunctionShortName}()'",
            ReplaceWithTopLevelFunction(replacements)
        )
    })

    private class ReplaceWithTopLevelFunction(private val replacements: List<Replacement>) : LocalQuickFix {
        override fun getName() = "Replace with '${replacements.first().kotlinFunctionShortName}()'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement ?: return
            val editor = element.findExistingEditor()
            if (editor != null && replacements.size > 1 && !ApplicationManager.getApplication().isUnitTestMode) {
                chooseAndApplyFix(editor, project, element)
            } else {
                applyFix(replacements.first(), project, element)
            }
        }

        private fun chooseAndApplyFix(editor: Editor, project: Project, element: PsiElement) {
            JBPopupFactory.getInstance().createListPopup(object : BaseListPopupStep<Replacement>("Choose function", replacements) {
                override fun onChosen(selectedValue: Replacement?, finalChoice: Boolean): PopupStep<String>? {
                    if (selectedValue == null || project.isDisposed) return null
                    project.executeWriteCommand(name) {
                        applyFix(selectedValue, project, element)
                    }
                    return null
                }

                override fun getTextFor(value: Replacement) = value.kotlinFunctionFqName
            }).showInBestPositionFor(editor)
        }

        private fun applyFix(replacement: Replacement, project: Project, element: PsiElement) {
            val dotQualified = element as? KtDotQualifiedExpression ?: return
            val call = dotQualified.callExpression ?: return
            val file = dotQualified.containingKtFile
            val psiFactory = KtPsiFactory(call)
            val valueArguments = call.valueArguments
            if (replacement.toExtensionFunction) {
                val receiverText = valueArguments.first().text
                val argumentsText = valueArguments.drop(1).joinToString(separator = ", ") { it.text }
                dotQualified.replaced(psiFactory.createExpression("$receiverText.${replacement.kotlinFunctionShortName}($argumentsText)"))
                file.resolveImportReference(FqName(replacement.kotlinFunctionFqName)).firstOrNull()?.let {
                    ImportInsertHelper.getInstance(project).importDescriptor(file, it)
                }
            } else {
                val argumentsText = valueArguments.joinToString(separator = ", ") { it.text }
                val replaced = dotQualified.replaced(psiFactory.createExpression("${replacement.kotlinFunctionFqName}($argumentsText)"))
                ShortenReferences.DEFAULT.process(replaced)
            }
        }
    }

    private data class Replacement(
        val javaMethodFqName: String,
        val kotlinFunctionFqName: String,
        val toExtensionFunction: Boolean = false
    ) {
        private fun String.shortName() = takeLastWhile { it != '.' }

        val javaMethodShortName = javaMethodFqName.shortName()

        val kotlinFunctionShortName = kotlinFunctionFqName.shortName()
    }

    companion object {
        private val JAVA_PRIMITIVES = listOf(
            "Integer" to "Int",
            "Long" to "Long",
            "Byte" to "Byte",
            "Character" to "Char",
            "Short" to "Short",
            "Double" to "Double",
            "Float" to "Float"
        ).flatMap {
            listOf(
                Replacement("java.lang.${it.first}.toString", "kotlin.text.toString", toExtensionFunction = true),
                Replacement("java.lang.${it.first}.compare", "kotlin.primitives.${it.second}.compareTo", toExtensionFunction = true)
            )
        }

        private val JAVA_IO = listOf(
            Replacement("java.io.PrintStream.print", "kotlin.io.print"),
            Replacement("java.io.PrintStream.println", "kotlin.io.println")
        )

        private val JAVA_SYSTEM = listOf(
            Replacement("java.lang.System.exit", "kotlin.system.exitProcess")
        )

        private val JAVA_MATH = listOf(
            Replacement("java.lang.Math.abs", "kotlin.math.abs"),
            Replacement("java.lang.Math.acos", "kotlin.math.acos"),
            Replacement("java.lang.Math.asin", "kotlin.math.asin"),
            Replacement("java.lang.Math.atan", "kotlin.math.atan"),
            Replacement("java.lang.Math.atan2", "kotlin.math.atan2"),
            Replacement("java.lang.Math.ceil", "kotlin.math.ceil"),
            Replacement("java.lang.Math.cos", "kotlin.math.cos"),
            Replacement("java.lang.Math.cosh", "kotlin.math.cosh"),
            Replacement("java.lang.Math.exp", "kotlin.math.exp"),
            Replacement("java.lang.Math.expm1", "kotlin.math.expm1"),
            Replacement("java.lang.Math.floor", "kotlin.math.floor"),
            Replacement("java.lang.Math.hypot", "kotlin.math.hypot"),
            Replacement("java.lang.Math.IEEEremainder", "kotlin.math.IEEErem", toExtensionFunction = true),
            Replacement("java.lang.Math.log", "kotlin.math.ln"),
            Replacement("java.lang.Math.log1p", "kotlin.math.ln1p"),
            Replacement("java.lang.Math.log10", "kotlin.math.log10"),
            Replacement("java.lang.Math.max", "kotlin.math.max"),
            Replacement("java.lang.Math.min", "kotlin.math.min"),
            Replacement("java.lang.Math.nextDown", "kotlin.math.nextDown", toExtensionFunction = true),
            Replacement("java.lang.Math.nextAfter", "kotlin.math.nextTowards", toExtensionFunction = true),
            Replacement("java.lang.Math.nextUp", "kotlin.math.nextUp", toExtensionFunction = true),
            Replacement("java.lang.Math.pow", "kotlin.math.pow", toExtensionFunction = true),
            Replacement("java.lang.Math.rint", "kotlin.math.round"),
            Replacement("java.lang.Math.round", "kotlin.math.roundToLong", toExtensionFunction = true),
            Replacement("java.lang.Math.round", "kotlin.math.roundToInt", toExtensionFunction = true),
            Replacement("java.lang.Math.signum", "kotlin.math.sign"),
            Replacement("java.lang.Math.sin", "kotlin.math.sin"),
            Replacement("java.lang.Math.sinh", "kotlin.math.sinh"),
            Replacement("java.lang.Math.sqrt", "kotlin.math.sqrt"),
            Replacement("java.lang.Math.tan", "kotlin.math.tan"),
            Replacement("java.lang.Math.tanh", "kotlin.math.tanh"),
            Replacement("java.lang.Math.copySign", "kotlin.math.withSign", toExtensionFunction = true)
        )

        private val JAVA_ARRAYS = listOf(Replacement("java.util.Arrays.copyOf", "kotlin.collections.copyOf", toExtensionFunction = true))

        private val REPLACEMENTS = (JAVA_MATH + JAVA_SYSTEM + JAVA_IO + JAVA_PRIMITIVES + JAVA_ARRAYS).groupBy { it.javaMethodShortName }
    }
}
