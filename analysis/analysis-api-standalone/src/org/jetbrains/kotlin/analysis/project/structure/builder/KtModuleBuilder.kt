/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.platform.TargetPlatform

@KtModuleBuilderDsl
public abstract class KtModuleBuilder {
    public val directRegularDependencies: MutableList<KtModule> = mutableListOf()
    public val directRefinementDependencies: MutableList<KtModule> = mutableListOf()
    public val directFriendDependencies: MutableList<KtModule> = mutableListOf()

    public lateinit var contentScope: GlobalSearchScope
    public lateinit var platform: TargetPlatform
    public lateinit var project: Project

    public abstract fun build(): KtModule
}