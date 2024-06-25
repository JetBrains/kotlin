/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.platform.TargetPlatform

@KaPlatformInterface
@KaImplementationDetail
class KaBuiltinsModuleImpl(
    override val targetPlatform: TargetPlatform,
    override val project: Project,
) : KaBuiltinsModule {
    override val contentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override fun equals(other: Any?): Boolean = other is KaBuiltinsModule && this.targetPlatform == other.targetPlatform
    override fun hashCode(): Int = targetPlatform.hashCode()
}