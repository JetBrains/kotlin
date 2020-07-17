/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.canDropBraces
import org.jetbrains.kotlin.idea.core.dropBraces
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class ReplaceStringFormatWithLiteralInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor(fun(callExpression) {
            if (callExpression.calleeExpression?.text != "format") return
            val qualifiedExpression = callExpression.parent as? KtQualifiedExpression
            if (qualifiedExpression != null && !qualifiedExpression.receiverExpression.text.endsWith("String")) return

            val args = callExpression.valueArguments.mapNotNull { it.getArgumentExpression() }
            if (args.size <= 1) return

            val format = args[0].text
            if (format.startsWith("\"\"\"")) return

            val fqName = callExpression.getCallableDescriptor()?.importableFqName?.asString() ?: return
            if (fqName != "kotlin.text.format" && fqName != "java.lang.String.format") return

            val placeHolders = placeHolder.findAll(format).toList()
            if (placeHolders.size != args.size - 1) return
            placeHolders.forEach {
                val next = it.range.last + 1
                val nextStr = if (next < format.length) format.substring(next, next + 1) else null
                if (nextStr != "s") return
            }

            val context = callExpression.analyze(BodyResolveMode.PARTIAL)
            if (args.asSequence().drop(1).any { it.isSubtypeOfFormattable(context) }) return

            holder.registerProblem(
                qualifiedExpression ?: callExpression,
                KotlinBundle.message("string.format.call.can.be.replaced.with.string.templates"),
                ProblemHighlightType.INFORMATION,
                ReplaceWithStringLiteralFix()
            )
        })
    }

    private companion object {
        private val placeHolder: Regex by lazy { "%".toRegex() }
        private val stringPlaceHolder: Regex by lazy { "%s".toRegex() }
    }

    private class ReplaceWithStringLiteralFix : LocalQuickFix {
        override fun getFamilyName() = KotlinBundle.message("replace.with.string.literal.fix.family.name")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val qualifiedExpression = element as? KtQualifiedExpression
            val callExpression = qualifiedExpression?.callExpression ?: element as? KtCallExpression ?: return

            val args = callExpression.valueArguments.mapNotNull { it.getArgumentExpression() }
            val format = args[0].text.removePrefix("\"").removeSuffix("\"")
            val replaceArgs = args.asSequence().drop(1).mapTo(LinkedList()) { ConvertToStringTemplateIntention.buildText(it, true) }
            val stringLiteral = stringPlaceHolder.replace(format) { replaceArgs.pop() }
            (qualifiedExpression ?: callExpression)
                .let { it.replaced(KtPsiFactory(it).createStringTemplate(stringLiteral)) }
                .entries
                .forEach {
                    val blockEntry = (it as? KtBlockStringTemplateEntry)
                    if (blockEntry?.canDropBraces() == true) blockEntry.dropBraces()
                }
        }
    }
}

private fun KtExpression.isSubtypeOfFormattable(context: BindingContext): Boolean {
    return getType(context)?.constructor?.supertypes?.reversed()?.any {
        it.constructor.declarationDescriptor?.fqNameSafe?.asString() == "java.util.Formattable"
    } == true
}