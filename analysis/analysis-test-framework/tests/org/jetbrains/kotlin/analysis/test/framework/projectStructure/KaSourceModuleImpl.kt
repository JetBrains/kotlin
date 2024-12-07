/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform

class KaSourceModuleImpl(
    override val name: String,
    override val targetPlatform: TargetPlatform,
    override val languageVersionSettings: LanguageVersionSettings,
    override val project: Project,
    override val contentScope: GlobalSearchScope,
) : KtModuleWithModifiableDependencies(), KaSourceModule {
    override val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<KaModule> = mutableListOf()

    override fun toString(): String = name

    @KaExperimentalApi
    override val psiRoots: List<PsiFileSystemItem>
        get() = listOf()
}
