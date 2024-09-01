/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.computeTransitiveDependsOnDependencies
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile

@KaExperimentalApi
internal class KaScriptModuleImpl(
    override val directRegularDependencies: List<KaModule> = emptyList(),
    override val directDependsOnDependencies: List<KaModule> = emptyList(),
    override val directFriendDependencies: List<KaModule> = emptyList(),
    override val targetPlatform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    override val project: Project,
    override val file: KtFile,
    override val languageVersionSettings: LanguageVersionSettings
) : KaScriptModule, KtModuleWithPlatform {
    override val transitiveDependsOnDependencies: List<KaModule> by lazy {
        computeTransitiveDependsOnDependencies(directDependsOnDependencies)
    }

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)
}
