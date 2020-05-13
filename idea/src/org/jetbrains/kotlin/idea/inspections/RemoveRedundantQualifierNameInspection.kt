/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.unwrapIfFakeOverride
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveToDescriptors
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.JComponent

class RemoveRedundantQualifierNameInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    companion object {
        private val ENUM_STATIC_METHODS = listOf("values", "valueOf")
    }

    /**
     * In order to detect that `foo()` and `GrandBase.foo()` point to the same method,
     * we need to unwrap fake overrides from descriptors. If we don't do that, they will
     * have different `fqName`s, and the inspection will not detect `GrandBase` as a
     * redundant qualifier.
     */
    var unwrapFakeOverrides: Boolean = false

    override fun createOptionsPanel(): JComponent =
        SingleCheckboxOptionsPanel(
            KotlinBundle.message("redundant.qualifier.unnecessary.non.direct.parent.class.qualifier"),
            this,
            ::unwrapFakeOverrides.name
        )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val expressionParent = expression.parent
                if (expressionParent is KtDotQualifiedExpression || expressionParent is KtPackageDirective || expressionParent is KtImportDirective) return
                val expressionForAnalyze = expression.firstExpressionWithoutReceiver() ?: return

                val originalExpression: KtExpression = expressionForAnalyze.parent as? KtClassLiteralExpression ?: expressionForAnalyze

                val receiverReference = expressionForAnalyze.receiverExpression.let {
                    it.safeAs<KtQualifiedExpression>()?.receiverExpression ?: it
                }.mainReference?.resolve()

                val parentEnumEntry = expressionForAnalyze.getStrictParentOfType<KtEnumEntry>()
                if (parentEnumEntry != null) {
                    val companionObject = (receiverReference as? KtObjectDeclaration)?.takeIf { it.isCompanion() }
                    if (companionObject?.containingClass() == parentEnumEntry.getStrictParentOfType<KtClass>()) return
                }

                if (receiverReference?.safeAs<KtClass>()?.isEnum() == true
                    && expressionForAnalyze.getParentOfTypesAndPredicate(true, KtClass::class.java) { it.isEnum() } != receiverReference
                    && (expressionForAnalyze.isEnumStaticMethodCall() || expressionForAnalyze.isCompanionObjectReference())
                ) return

                val context = originalExpression.analyze()

                val originalDescriptor = expressionForAnalyze.getQualifiedElementSelector()
                    ?.mainReference?.resolveToDescriptors(context)
                    ?.firstOrNull() ?: return

                val applicableExpression = expressionForAnalyze.firstApplicableExpression(
                    validator = { applicableExpression(originalExpression, context, originalDescriptor, unwrapFakeOverrides) },
                    generator = { firstChild as? KtDotQualifiedExpression }
                ) ?: return

                reportProblem(holder, applicableExpression)
            }

            override fun visitUserType(type: KtUserType) {
                if (type.parent is KtUserType) return

                val context = type.analyze()
                val applicableExpression = type.firstApplicableExpression(
                    validator = { applicableExpression(context) },
                    generator = { firstChild as? KtUserType }
                ) ?: return

                reportProblem(holder, applicableExpression)
            }
        }

    private fun KtDotQualifiedExpression.isEnumStaticMethodCall() = callExpression?.calleeExpression?.text in ENUM_STATIC_METHODS

    private fun KtDotQualifiedExpression.isCompanionObjectReference(): Boolean {
        val selector = receiverExpression.safeAs<KtDotQualifiedExpression>()?.selectorExpression ?: selectorExpression
        return selector?.referenceExpression()?.mainReference?.resolve()?.safeAs<KtObjectDeclaration>()?.isCompanion() == true
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
    originalDescriptor: DeclarationDescriptor,
    unwrapFakeOverrides: Boolean
): KtDotQualifiedExpression? {
    if (!receiverExpression.isApplicableReceiver(oldContext) || !ShortenReferences.canBePossibleToDropReceiver(this, oldContext)) {
        return null
    }

    val expressionText = originalExpression.text.substring(lastChild.startOffset - originalExpression.startOffset)
    val newExpression = KtPsiFactory(originalExpression).createExpressionIfPossible(expressionText) ?: return null
    val newContext = newExpression.analyzeAsReplacement(originalExpression, oldContext)
    val newDescriptor = newExpression.selector()
        ?.mainReference?.resolveToDescriptors(newContext)
        ?.firstOrNull() ?: return null

    fun DeclarationDescriptor.unwrapFakeOverrideIfNecessary(): DeclarationDescriptor {
        return if (unwrapFakeOverrides) this.unwrapIfFakeOverride() else this
    }

    val originalDescriptorFqName = originalDescriptor.unwrapFakeOverrideIfNecessary().fqNameSafe
    val newDescriptorFqName = newDescriptor.unwrapFakeOverrideIfNecessary().fqNameSafe
    if (originalDescriptorFqName != newDescriptorFqName) return null

    return this.takeIf {
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
        KotlinBundle.message("redundant.qualifier.name"),
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        TextRange.from(firstChild.startOffsetInParent, firstChild.textLength + 1),
        RemoveRedundantQualifierNameQuickFix()
    )
}

class RemoveRedundantQualifierNameQuickFix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("remove.redundant.qualifier.name.quick.fix.text")
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
        Regex.fromLiteral(substring).findAll(file.text, file.importList?.endOffset ?: 0).toList().asReversed().forEach {
            ShortenReferences.DEFAULT.process(file, it.range.first, it.range.last + 1)
        }
    }
}