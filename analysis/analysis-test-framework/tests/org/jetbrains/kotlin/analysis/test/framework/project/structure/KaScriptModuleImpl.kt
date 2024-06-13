/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

class KaScriptModuleImpl(
    override val file: KtFile,
    override val targetPlatform: TargetPlatform,
    override val languageVersionSettings: LanguageVersionSettings,
    override val project: Project,
) : KtModuleWithModifiableDependencies(), KaScriptModule {
    override val contentScope: GlobalSearchScope get() = GlobalSearchScope.fileScope(file)
    override val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<KaModule> = mutableListOf()
}
