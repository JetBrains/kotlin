/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KaLibrarySourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.builder.KtLibrarySourceModuleBuilder

internal class KtLibrarySourceModuleBuilderImpl(private val project: Project) : KtLibrarySourceModuleBuilder() {
    override fun build(): KaLibrarySourceModule {
        return KaLibrarySourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            libraryName,
            binaryLibrary
        )
    }
}
