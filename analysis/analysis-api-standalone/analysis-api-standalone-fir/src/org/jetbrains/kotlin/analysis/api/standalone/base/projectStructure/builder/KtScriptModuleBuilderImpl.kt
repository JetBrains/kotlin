/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KaScriptModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtScriptModuleBuilder

internal class KtScriptModuleBuilderImpl(private val project: Project) : KtScriptModuleBuilder() {
    override fun build(): KaScriptModule {
        return KaScriptModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            platform,
            project,
            file,
            languageVersionSettings
        )
    }
}
