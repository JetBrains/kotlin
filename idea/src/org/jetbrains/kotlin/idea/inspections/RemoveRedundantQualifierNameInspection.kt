/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.hasNotReceiver
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstClassifierWithDeprecationStatus

class RemoveRedundantQualifierNameInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val expressionParent = expression.parent
                if (expressionParent is KtDotQualifiedExpression || expressionParent is KtPackageDirective || expressionParent is KtImportDirective) return
                val expressionForAnalyze = expression.firstExpressionWithoutReceiver() ?: return

                val parent = expressionForAnalyze.parent
                @Suppress("USELESS_CAST") val originalExpression = if (parent is KtClassLiteralExpression)
                    parent as KtExpression
                else
                    expressionForAnalyze

                val parentEnumEntry = expressionForAnalyze.getStrictParentOfType<KtEnumEntry>()
                if (parentEnumEntry != null) {
                    val companionObject = (expressionForAnalyze.receiverExpression.mainReference?.resolve() as? KtObjectDeclaration)
                        ?.takeIf { it.isCompanion() }
                    if (companionObject?.containingClass() == parentEnumEntry.getStrictParentOfType<KtClass>()) return
                }

                val context = originalExpression.analyze()

                val originalDescriptor = expressionForAnalyze.getQualifiedElementSelector()
                    ?.mainReference?.resolveToDescriptors(context)
                    ?.firstOrNull() ?: return

                val applicableExpression = expressionForAnalyze.firstApplicableExpression(validator = {
                    applicableExpression(originalExpression, context, originalDescriptor)
                }) {
                    firstChild as? KtDotQualifiedExpression
                } ?: return

                reportProblem(holder, applicableExpression)
            }

            override fun visitUserType(type: KtUserType) {
                if (type.parent is KtUserType) return

                val context = type.analyze()
                val applicableExpression = type.firstApplicableExpression(validator = { applicableExpression(context) }) {
                    firstChild as? KtUserType
                } ?: return

                reportProblem(holder, applicableExpression)
            }
        }
}

private tailrec fun KtDotQualifiedExpression.firstExpressionWithoutReceiver(): KtDotQualifiedExpression? = if (hasNotReceiver())
    this
else
    (receiverExpression as? KtDotQualifiedExpression)?.firstExpressionWithoutReceiver()

private tailrec fun <T : KtElement> T.firstApplicableExpression(validator: T.() -> T?, generator: T.() -> T?): T? =
    validator() ?: generator()?.firstApplicableExpression(validator, generator)

private fun KtDotQualifiedExpression.applicableExpression(
    originalExpression: KtExpression,
    oldContext: BindingContext,
    originalDescriptor: DeclarationDescriptor
): KtDotQualifiedExpression? {
    if (!receiverExpression.isApplicableReceiver(oldContext) || !ShortenReferences.canBePossibleToDropReceiver(
            this,
            oldContext
        )
    ) return null
    val expressionText = originalExpression.text.substring(lastChild.startOffset - originalExpression.startOffset)
    val newExpression = KtPsiFactory(originalExpression).createExpressionIfPossible(expressionText) ?: return null
    val newContext = newExpression.analyzeAsReplacement(originalExpression, oldContext)
    val newDescriptor = newExpression.selector()
        ?.mainReference?.resolveToDescriptors(newContext)
        ?.firstOrNull() ?: return null

    return takeIf {
        originalDescriptor.fqNameSafe == newDescriptor.fqNameSafe &&
                if (newDescriptor is ImportedFromObjectCallableDescriptor<*>)
                    compareDescriptors(project, newDescriptor.callableFromObject, originalDescriptor)
                else
                    compareDescriptors(project, newDescriptor, originalDescriptor)
    }
}

private fun KtExpression.selector(): KtElement? = if (this is KtClassLiteralExpression) receiverExpression?.getQualifiedElementSelector()
else getQualifiedElementSelector()

private fun KtExpression.isApplicableReceiver(context: BindingContext): Boolean {
    if (this is KtInstanceExpressionWithLabel) return false

    val reference = getQualifiedElementSelector()
    val descriptor = reference?.mainReference?.resolveToDescriptors(context)?.firstOrNull() ?: return false

    return if (!descriptor.isCompanionObject()) true
    else descriptor.name.asString() != reference.text
}

private fun KtUserType.applicableExpression(context: BindingContext): KtUserType? {
    if (firstChild !is KtUserType) return null
    val referenceExpression = referenceExpression as? KtNameReferenceExpression ?: return null
    val originalDescriptor = referenceExpression.mainReference.resolveToDescriptors(context).firstOrNull() ?: return null

    if (originalDescriptor is ClassDescriptor
        && originalDescriptor.isInner
        && (originalDescriptor.containingDeclaration as? ClassDescriptor)?.typeConstructor != null
    ) return null

    val shortName = originalDescriptor.importableFqName?.shortName() ?: return null
    val scope = referenceExpression.getResolutionScope(context) ?: return null
    val descriptor = scope.findFirstClassifierWithDeprecationStatus(shortName, NoLookupLocation.FROM_IDE)?.descriptor ?: return null
    return if (descriptor == originalDescriptor) this else null
}

private fun reportProblem(holder: ProblemsHolder, element: KtElement) {
    val firstChild = element.firstChild
    holder.registerProblem(
        element,
        "Redundant qualifier name",
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        TextRange.from(firstChild.startOffsetInParent, firstChild.textLength + 1),
        RemoveRedundantQualifierNameQuickFix()
    )
}

class RemoveRedundantQualifierNameQuickFix : LocalQuickFix {
    override fun getName() = "Remove redundant qualifier name"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile as KtFile
        val range = when (val element = descriptor.psiElement) {
            is KtUserType -> IntRange(element.startOffset, element.endOffset)
            is KtDotQualifiedExpression -> IntRange(
                element.startOffset,
                element.getLastParentOfTypeInRowWithSelf<KtDotQualifiedExpression>()?.getQualifiedElementSelector()?.endOffset ?: return
            )
            else -> IntRange.EMPTY
        }

        val substring = file.text.substring(range.first, range.last)
        Regex.fromLiteral(substring).findAll(file.text, file.importList?.endOffset ?: 0).toList().reversed().forEach {
            ShortenReferences.DEFAULT.process(file, it.range.first, it.range.last + 1)
        }
    }
}