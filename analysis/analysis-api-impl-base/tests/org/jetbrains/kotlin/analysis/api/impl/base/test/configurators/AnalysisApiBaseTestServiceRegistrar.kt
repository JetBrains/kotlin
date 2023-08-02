/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileClassProvider
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

object AnalysisApiBaseTestServiceRegistrar: AnalysisApiTestServiceRegistrar()  {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    @OptIn(KtAnalysisApiInternals::class)
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
            registerService(KotlinMessageBusProvider::class.java, KotlinProjectMessageBusProvider::class.java)
            registerService(KotlinGlobalModificationService::class.java, KotlinStaticGlobalModificationService::class.java)

            registerService(KtLifetimeTokenProvider::class.java, KtReadActionConfinementLifetimeTokenProvider::class.java)

            //KotlinClassFileDecompiler is registered as application service so it's available for the tests run in parallel as well
            //when the decompiler is registered, for compiled class KtClsFile is created instead of ClsFileImpl
            //and KtFile doesn't return any classes in classOwner.getClasses if there is no KtFileClassProvider
            //but getClasses is used during java resolve, thus it's required to return some PsiClass for such cases
            registerService(KtFileClassProvider::class.java, KtClsFileClassProvider(project))
            registerService(ClsJavaStubByVirtualFileCache::class.java, ClsJavaStubByVirtualFileCache())
        }
    }

    class KtClsFileClassProvider(val project: Project) : KtFileClassProvider {
        override fun getFileClasses(file: KtFile): Array<PsiClass> {
            val virtualFile = file.virtualFile
            val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
            if (file is KtClsFile && virtualFile != null) {
                DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(file, virtualFile, classOrObject, project)?.let {
                    return arrayOf(it)
                }
            }
            return PsiClass.EMPTY_ARRAY
        }
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        val moduleStructure = testServices.ktModuleProvider.getModuleStructure()
        val allSourceKtFiles = moduleStructure.mainModules.flatMap { it.files.filterIsInstance<KtFile>() }
        val roots = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(
            moduleStructure.binaryModules.flatMap { binary -> binary.getBinaryRoots() },
            testServices.environmentManager.getProjectEnvironment()
        ).distinct()
        project.apply {
            registerService(KtModuleScopeProvider::class.java, KtModuleScopeProviderImpl())
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(allSourceKtFiles))

            val filter = BuiltInDefinitionFile.FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES
            val ktFilesForBinaries: List<KtFile>
            try {
                BuiltInDefinitionFile.FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = false
                val declarationProviderFactory = KotlinStaticDeclarationProviderFactory(
                    project,
                    allSourceKtFiles,
                    additionalRoots = roots,
                    skipBuiltins = testServices.moduleStructure.allDirectives.contains(NO_RUNTIME),
                )
                ktFilesForBinaries = declarationProviderFactory.getAdditionalCreatedKtFiles()
                registerService(
                    KotlinDeclarationProviderFactory::class.java, declarationProviderFactory
                )
            } finally {
                BuiltInDefinitionFile.FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = filter
            }
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStaticDeclarationProviderMerger(project))
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStaticPackageProviderFactory(project, allSourceKtFiles + ktFilesForBinaries)
            )
            registerService(KotlinResolutionScopeProvider::class.java, KotlinByModulesResolutionScopeProvider::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        testServices.environmentManager.getApplicationEnvironment().registerFileType(KotlinBuiltInFileType, "kotlin_builtins")
        testServices.environmentManager.getApplicationEnvironment().registerFileType(KotlinBuiltInFileType, "kotlin_metadata")
    }
}
