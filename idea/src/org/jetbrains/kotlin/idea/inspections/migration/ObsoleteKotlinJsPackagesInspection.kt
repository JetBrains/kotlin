/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class ObsoleteKotlinJsPackagesInspection : ObsoleteCodeMigrationInspection() {
    override val fromVersion: LanguageVersion = LanguageVersion.KOTLIN_1_3
    override val toVersion: LanguageVersion = LanguageVersion.KOTLIN_1_4

    override val problemReporters: List<ObsoleteCodeProblemReporter> = listOf(
        KotlinBrowserFullyQualifiedUsageReporter,
        KotlinBrowserImportUsageReporter,
        KotlinDomImportUsageReporter
    )
}

private object ObsoleteKotlinJsPackagesUsagesInWholeProjectFix : ObsoleteCodeInWholeProjectFix() {
    override val inspectionName: String = ObsoleteKotlinJsPackagesInspection().shortName
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.kotlin.js.packages.usage.in.whole.fix.family.name")
}

private const val KOTLIN_BROWSER_PACKAGE = "kotlin.browser"
private const val KOTLINX_BROWSER_PACKAGE = "kotlinx.browser"
private const val KOTLIN_DOM_PACKAGE = "kotlin.dom"

private class ObsoleteKotlinBrowserUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.package.usage.fix.family.name", KOTLIN_BROWSER_PACKAGE)
}

private class ObsoleteKotlinDomUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.package.usage.fix.family.name", KOTLIN_DOM_PACKAGE)
}

/**
 * We have such inspection only for 'kotlin.browser' package; it is because 'kotlin.dom' package has only extension functions. Such
 * functions cannot be used with fully qualified name.
 */
private object KotlinBrowserFullyQualifiedUsageReporter : ObsoleteCodeProblemReporter {
    override fun report(holder: ProblemsHolder, isOnTheFly: Boolean, simpleNameExpression: KtSimpleNameExpression): Boolean {
        val fullyQualifiedExpression = simpleNameExpression.parent as? KtDotQualifiedExpression ?: return false

        val kotlinBrowserQualifier = fullyQualifiedExpression.receiverExpression as? KtDotQualifiedExpression ?: return false
        if (kotlinBrowserQualifier.text != KOTLIN_BROWSER_PACKAGE) return false

        if (!resolvesToKotlinBrowserPackage(simpleNameExpression)) return false

        holder.registerProblem(
            kotlinBrowserQualifier,
            KotlinBundle.message("package.usages.are.obsolete.since.1.4", KOTLIN_DOM_PACKAGE),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            *fixesWithWholeProject(
                isOnTheFly,
                fix = ObsoleteKotlinBrowserUsageFix(KotlinBrowserFullyQualifiedUsageFix),
                wholeProjectFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
            )
        )

        return true
    }

    private fun resolvesToKotlinBrowserPackage(simpleNameExpression: KtSimpleNameExpression): Boolean {
        val referencedDescriptor =
            simpleNameExpression.resolveMainReferenceToDescriptors().singleOrNull() as? CallableDescriptor ?: return false

        return referencedDescriptor.findPackage().fqName.asString() == KOTLIN_BROWSER_PACKAGE
    }

    object KotlinBrowserFullyQualifiedUsageFix : ObsoleteCodeFix {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val oldQualifier = descriptor.psiElement as? KtDotQualifiedExpression ?: return

            val newQualifier = KtPsiFactory(oldQualifier).createExpression(KOTLINX_BROWSER_PACKAGE)
            oldQualifier.replace(newQualifier)
        }
    }
}

private object KotlinBrowserImportUsageReporter : ObsoleteImportsUsageReporter() {
    override val textMarker: String = "browser"
    override val packageBindings: Map<String, String> = mapOf(KOTLIN_BROWSER_PACKAGE to KOTLINX_BROWSER_PACKAGE)

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("package.usages.are.obsolete.since.1.4", KOTLIN_BROWSER_PACKAGE)

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinBrowserUsageFix(fix)
}

private object KotlinDomImportUsageReporter : ObsoleteImportsUsageReporter() {
    override val textMarker: String = "dom"
    override val packageBindings: Map<String, String> = mapOf(KOTLIN_DOM_PACKAGE to "kotlinx.dom")

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("package.usages.are.obsolete.since.1.4", KOTLIN_DOM_PACKAGE)

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinDomUsageFix(fix)
}
