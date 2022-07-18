/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

abstract class AnalysisApiKtModuleProvider : TestService {
    protected abstract val testServices: TestServices
    abstract fun getModule(moduleName: String): KtModule

    abstract fun getModuleFiles(module: TestModule): List<PsiFile>

    abstract fun registerProjectStructure(modules: KtModuleProjectStructure)

    protected abstract fun getModuleName(ktModule: KtModule): String

    abstract fun getModuleStructure(): KtModuleProjectStructure
}

class AnalysisApiKtModuleProviderImpl(
    override val testServices: TestServices,
) : AnalysisApiKtModuleProvider() {
    private lateinit var modulesStructure: KtModuleProjectStructure
    private lateinit var modulesByName: Map<String, KtModuleWithFiles>

    override fun getModule(moduleName: String): KtModule {
        return modulesByName.getValue(moduleName).ktModule
    }

    override fun getModuleFiles(module: TestModule): List<PsiFile> = modulesByName.getValue(module.name).files

    override fun registerProjectStructure(modules: KtModuleProjectStructure) {
        require(!this::modulesStructure.isInitialized)
        require(!this::modulesByName.isInitialized)

        this.modulesStructure = modules
        this.modulesByName = modulesStructure.mainModules.associateBy { getModuleName(it.ktModule) }
    }

    override fun getModuleName(ktModule: KtModule): String = when (ktModule) {
        is KtLibraryModule -> ktModule.libraryName
        is KtSdkModule -> ktModule.sdkName
        is KtLibrarySourceModule -> ktModule.libraryName
        is KtSourceModule -> ktModule.moduleName
        is KtNotUnderContentRootModule -> TODO()
        is KtBuiltinsModule -> "Builtins for ${ktModule.platform}"
    }


    override fun getModuleStructure(): KtModuleProjectStructure = modulesStructure
}

val TestServices.ktModuleProvider: AnalysisApiKtModuleProvider by TestServices.testServiceAccessor()
