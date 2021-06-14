/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProviderByIndexesImpl

class FirModuleResolveStateConfiguratorIdeImpl(private val project: Project) : FirModuleResolveStateConfigurator() {
    override fun createDeclarationProvider(scope: GlobalSearchScope): DeclarationProvider =
        DeclarationProviderByIndexesImpl(project, scope)
}