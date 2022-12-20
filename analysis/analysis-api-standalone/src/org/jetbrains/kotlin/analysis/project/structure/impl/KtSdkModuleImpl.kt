/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.computeTransitiveDependsOnDependencies
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.nio.file.Path

internal class KtSdkModuleImpl(
    override val directRegularDependencies: List<KtModule>,
    override val directDependsOnDependencies: List<KtModule>,
    override val directFriendDependencies: List<KtModule>,
    override val contentScope: GlobalSearchScope,
    override val platform: TargetPlatform,
    override val project: Project,
    private val binaryRoots: Collection<Path>,
    override val sdkName: String,
) : KtSdkModule, KtModuleWithPlatform {
    override val transitiveDependsOnDependencies: List<KtModule> by lazy { computeTransitiveDependsOnDependencies(directDependsOnDependencies) }
    override val analyzerServices: PlatformDependentAnalyzerServices = super.analyzerServices

    override fun getBinaryRoots(): Collection<Path> = binaryRoots
}