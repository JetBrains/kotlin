/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinBundle

internal class ObsoleteKotlinJsPackagesInspection : ObsoleteCodeMigrationInspection() {
    override val fromVersion: LanguageVersion = LanguageVersion.KOTLIN_1_3
    override val toVersion: LanguageVersion = LanguageVersion.KOTLIN_1_4

    override val problems: List<ObsoleteCodeMigrationProblem> = listOf(
        KotlinBrowserImportUsageProblem,
        KotlinDomImportUsageProblem
    )
}

private object ObsoleteKotlinJsPackagesUsagesInWholeProjectFix : ObsoleteCodeInWholeProjectFix() {
    override val inspectionName: String = ObsoleteKotlinJsPackagesInspection().shortName
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.kotlin.js.packages.usage.in.whole.fix.family.name")
}

private class ObsoleteKotlinBrowserUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.kotlin.browser.usage.fix.family.name")
}

private class ObsoleteKotlinDomUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.kotlin.dom.usage.fix.family.name")
}

private object KotlinBrowserImportUsageProblem : ObsoleteImportsUsage() {
    override val textMarker: String = "browser"
    override val packageBindings: Map<String, String> = mapOf("kotlin.browser" to "kotlinx.browser")

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("kotlin.browser.usages.are.obsolete.since.1.3")

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinBrowserUsageFix(fix)
}

private object KotlinDomImportUsageProblem : ObsoleteImportsUsage() {
    override val textMarker: String = "dom"
    override val packageBindings: Map<String, String> = mapOf("kotlin.dom" to "kotlinx.dom")

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("kotlin.dom.usages.are.obsolete.since.1.3")

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinDomUsageFix(fix)
}
