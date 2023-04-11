/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.util

import com.intellij.mock.MockProject
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KtResolveExtension
import org.jetbrains.kotlin.analysis.providers.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.providers.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class KtResolveAbstentionProviderForTest(
    private val files: List<KtResolveExtensionFile>,
    private val packages: Set<FqName>,
) : KtResolveExtensionProvider() {
    override fun provideExtensionsFor(module: KtModule): List<KtResolveExtension> {
        return listOf(KtResolveExtensionForTest(files, packages))
    }
}

class KtResolveAbstentionProviderForTestPreAnalysisHandler(
    testServices: TestServices,
    private val providers: List<KtResolveAbstentionProviderForTest>,
) : PreAnalysisHandler(testServices) {
    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        val project = testServices.environmentManager.getProject() as MockProject
        val extensionPoint = project.extensionArea.getExtensionPoint(KtResolveExtensionProvider.EP_NAME)
        for (provider in providers) {
            extensionPoint.registerExtension(provider, project)
        }
    }
}


class KtResolveExtensionForTest(
    private val files: List<KtResolveExtensionFile>,
    private val packages: Set<FqName>,
) : KtResolveExtension() {
    override fun getKtFiles(): List<KtResolveExtensionFile> = files
    override fun getModificationTracker(): ModificationTracker = ModificationTracker.NEVER_CHANGED
    override fun getPackagesToBeResolved(): Set<FqName> = packages
}