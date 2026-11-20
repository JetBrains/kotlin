/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinModuleMappingFileProvider
import org.jetbrains.kotlin.analysis.api.standalone.fir.packages.KotlinStandaloneModuleMappingFileProvider
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class TestKotlinModuleMappingFileProvider(private val testServices: TestServices) : KotlinModuleMappingFileProvider {
    private val delegate by lazy {
        val binaryRoots = StandaloneProjectFactory.getAllBinaryRoots(
            testServices.ktTestModuleStructure.mainAndBinaryKtModules,
            testServices.environmentManager.getApplicationEnvironment(),
        )

        KotlinStandaloneModuleMappingFileProvider(binaryRoots.map { it.file })
    }

    override fun findModuleMappingFiles(scope: GlobalSearchScope): Collection<VirtualFile> = delegate.findModuleMappingFiles(scope)
}
