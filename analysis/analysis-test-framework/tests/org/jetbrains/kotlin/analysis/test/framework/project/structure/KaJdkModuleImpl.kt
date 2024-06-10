/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSdkModule
import org.jetbrains.kotlin.platform.TargetPlatform
import java.nio.file.Path

class KaJdkModuleImpl(
    override val sdkName: String,
    override val platform: TargetPlatform,
    override val contentScope: GlobalSearchScope,
    override val project: Project,
    private val binaryRoots: Collection<Path>,
) : KtModuleWithModifiableDependencies(), KaSdkModule {
    override fun getBinaryRoots(): Collection<Path> = binaryRoots

    override val directRegularDependencies: MutableList<KaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<KaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<KaModule> = mutableListOf()
}
