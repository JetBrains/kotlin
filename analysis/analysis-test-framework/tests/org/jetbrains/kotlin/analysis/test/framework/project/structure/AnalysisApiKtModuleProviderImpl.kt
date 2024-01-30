/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AnalysisApiKtModuleProvider : TestService {
    protected abstract val testServices: TestServices
    abstract fun getModule(moduleName: String): KtModule

    abstract fun getModuleFiles(module: TestModule): List<PsiFile>

    abstract fun registerProjectStructure(modules: KtModuleProjectStructure)

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

    override fun getModuleFiles(module: TestModule): List<PsiFile> =
        (modulesByName[module.name] ?: modulesByName.getValue(module.files.single().name)).files

    override fun registerProjectStructure(modules: KtModuleProjectStructure) {
        require(!this::modulesStructure.isInitialized)
        require(!this::modulesByName.isInitialized)

        this.modulesStructure = modules
        this.modulesByName = modulesStructure.mainModules.associateByName()
    }

    override fun getModuleStructure(): KtModuleProjectStructure = modulesStructure
}

fun AnalysisApiKtModuleProvider.getKtFiles(module: TestModule): List<KtFile> = getModuleFiles(module).filterIsInstance<KtFile>()

fun TestServices.allKtFiles(): List<KtFile> = moduleStructure.modules.flatMap(ktModuleProvider::getKtFiles)

val TestServices.ktModuleProvider: AnalysisApiKtModuleProvider by TestServices.testServiceAccessor()

fun List<KtModuleWithFiles>.associateByName(): Map<String, KtModuleWithFiles> {
    return associateBy { (ktModule, _) ->
        when (ktModule) {
            is KtModuleByCompilerConfiguration -> ktModule.moduleName
            is KtSourceModule -> ktModule.moduleName
            is KtLibraryModule -> ktModule.libraryName
            is KtLibrarySourceModule -> ktModule.libraryName
            is KtSdkModule -> ktModule.sdkName
            is KtBuiltinsModule -> "Builtins for ${ktModule.platform}"
            is KtNotUnderContentRootModule -> ktModule.name
            is KtScriptModule -> ktModule.file.name
            is KtDanglingFileModule -> ktModule.file.name
            else -> error("Unsupported module type: " + ktModule.javaClass.name)
        }
    }
}
