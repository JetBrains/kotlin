/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile


private val exclusions =
    listOf(
        "kotlin.jvm.internal",
        "kotlin.coroutines.experimental.intrinsics",
        "kotlin.coroutines.intrinsics",
        "kotlin.coroutines.experimental.jvm.internal",
        "kotlin.coroutines.jvm.internal",
        "kotlin.reflect.jvm.internal"
    )

private fun shouldBeHiddenAsInternalImplementationDetail(fqName: String, locationFqName: String) =
    exclusions.any { fqName.startsWith(it) } && (locationFqName.isBlank() || !fqName.startsWith(locationFqName))

/**
 * We do not want to show nothing from "kotlin.coroutines.experimental" when release coroutines are available,
 * since in 1.3 this package is obsolete.
 *
 * However, we still want to show this package when release coroutines are not available.
 */
private fun usesOutdatedCoroutinesPackage(fqName: String, languageVersionSettings: LanguageVersionSettings): Boolean =
    fqName.startsWith("kotlin.coroutines.experimental.") &&
            languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)

fun FqName.isExcludedFromAutoImport(project: Project, inFile: KtFile?, languageVersionSettings: LanguageVersionSettings?): Boolean {
    val fqName = this.asString()
    return JavaProjectCodeInsightSettings.getSettings(project).isExcluded(fqName) ||
            (languageVersionSettings != null && usesOutdatedCoroutinesPackage(fqName, languageVersionSettings)) ||
            shouldBeHiddenAsInternalImplementationDetail(fqName, inFile?.packageFqName?.asString() ?: "")
}
