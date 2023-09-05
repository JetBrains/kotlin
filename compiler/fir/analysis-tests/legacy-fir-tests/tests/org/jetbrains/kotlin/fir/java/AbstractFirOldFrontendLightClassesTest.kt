/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.doFirResolveTestBench
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.toSourceLinesMapping
import java.io.File

abstract class AbstractFirOldFrontendLightClassesTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        if (files.any { "FIR_IGNORE" in it.directives }) return
        try {
            analyzeAndCheckUnhandled(testDataFile, files, useLightTree)
        } catch (t: AssertionError) {
            throw t
        } catch (t: Throwable) {
            throw t
        }
    }

    private val useLightTree: Boolean
        get() = false

    private val useLazyBodiesModeForRawFir: Boolean
        get() = false

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        PsiElementFinder.EP.getPoint(environment.project).unregisterExtension(JavaElementFinder::class.java)
    }

    private fun analyzeAndCheckUnhandled(testDataFile: File, files: List<TestFile>, useLightTree: Boolean = false) {
        val groupedByModule = files.groupBy(TestFile::module)

        val modules = createModules(groupedByModule)

        val sessionProvider = FirProjectSessionProvider()

        //For BuiltIns, registered in sessionProvider automatically
        val allProjectScope = GlobalSearchScope.allScope(project)

        val configToSession = modules.mapValues { (config, info) ->
            val moduleFiles = groupedByModule.getValue(config)
            val scope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(
                project,
                moduleFiles.mapNotNull { it.ktFile }
            )
            val projectEnvironment = VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ) { environment.createPackagePartProvider(it) }

            FirSessionFactoryHelper.createSessionWithDependencies(
                Name.identifier(info.name.asString().removeSurrounding("<", ">")),
                info.platform,
                info.analyzerServices,
                externalSessionProvider = sessionProvider,
                projectEnvironment,
                config?.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT,
                javaSourcesScope = PsiBasedProjectFileSearchScope(scope),
                librariesScope = PsiBasedProjectFileSearchScope(allProjectScope),
                lookupTracker = null,
                enumWhenTracker = null,
                importTracker = null,
                incrementalCompilationContext = null,
                extensionRegistrars = emptyList(),
                needRegisterJavaElementFinder = true
            ) {}
        }

        val firFilesPerSession = mutableMapOf<FirSession, List<FirFile>>()

        // TODO: make module/session/transformer handling like in AbstractFirMultiModuleTest (IDE)
        for ((testModule, testFilesInModule) in groupedByModule) {
            val ktFiles = getKtFiles(testFilesInModule, true)

            val session = configToSession.getValue(testModule)

            val firFiles = mutableListOf<FirFile>()
            mapKtFilesToFirFiles(session, ktFiles, firFiles, useLightTree)
            firFilesPerSession[session] = firFiles
        }

        runAnalysis(testDataFile, firFilesPerSession)
    }

    private fun mapKtFilesToFirFiles(session: FirSession, ktFiles: List<KtFile>, firFiles: MutableList<FirFile>, useLightTree: Boolean) {
        val firProvider = (session.firProvider as FirProviderImpl)
        if (useLightTree) {
            val lightTreeBuilder = LightTree2Fir(session, firProvider.kotlinScopeProvider)
            ktFiles.mapTo(firFiles) {
                val firFile =
                    lightTreeBuilder.buildFirFile(
                        it.text,
                        KtInMemoryTextSourceFile(it.name, it.virtualFilePath, it.text),
                        it.text.toSourceLinesMapping()
                    )
                (session.firProvider as FirProviderImpl).recordFile(firFile)
                firFile
            }
        } else {
            val firBuilder = PsiRawFirBuilder(
                session,
                firProvider.kotlinScopeProvider,
                bodyBuildingMode = BodyBuildingMode.lazyBodies(useLazyBodiesModeForRawFir)
            )
            ktFiles.mapTo(firFiles) {
                val firFile = firBuilder.buildFirFile(it)
                firProvider.recordFile(firFile)
                firFile
            }
        }
    }

    private fun runAnalysis(testDataFile: File, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        for ((session, firFiles) in firFilesPerSession) {
            doFirResolveTestBench(firFiles, createAllCompilerResolveProcessors(session), gc = false)
        }
        checkResultingFirFiles(testDataFile)
    }

    private fun checkResultingFirFiles(testDataFile: File) {
        val ourFinders = PsiElementFinder.EP.getPoint(project).extensions.filterIsInstance<FirJavaElementFinder>()

        assertNotEmpty(ourFinders)

        val stringBuilder = StringBuilder()

        for (qualifiedName in InTextDirectivesUtils.findListWithPrefixes(testDataFile.readText(), "// LIGHT_CLASS_FQ_NAME: ")) {
            val fqName = FqName(qualifiedName)
            val packageName = fqName.parent().asString()

            val ourFinder = ourFinders.firstOrNull { finder -> finder.findPackage(packageName) != null }
            assertNotNull("PsiPackage for ${fqName.parent()} was not found", ourFinder)
            ourFinder!!

            val psiPackage = ourFinder.findPackage(fqName.parent().asString())
            assertNotNull("PsiPackage for ${fqName.parent()} is null", psiPackage)

            val psiClass = assertInstanceOf(
                ourFinder.findClass(qualifiedName, GlobalSearchScope.allScope(project)),
                ClsClassImpl::class.java
            )

            psiClass.appendMirrorText(0, stringBuilder)
            stringBuilder.appendLine()
        }

        val expectedPath = testDataFile.path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), stringBuilder.toString())
    }

    override fun createTestFileFromPath(filePath: String): File {
        return File(filePath)
    }

    private fun createModules(groupedByModule: Map<TestModule?, List<TestFile>>): MutableMap<TestModule?, ModuleInfo> {
        val modules =
            HashMap<TestModule?, ModuleInfo>()

        for (testModule in groupedByModule.keys) {
            val module = if (testModule == null)
                createSealedModule()
            else
                createModule(testModule.name)

            modules[testModule] = module
        }

        for (testModule in groupedByModule.keys) {
            if (testModule == null) continue

            val module = modules[testModule]!!
            val dependencies = ArrayList<ModuleInfo>()
            dependencies.add(module)
            for (dependency in testModule.dependencies) {
                dependencies.add(modules[dependency as TestModule?]!!)
            }


            dependencies.add(builtInsModuleInfo)
            (module as TestModuleInfo).dependencies.addAll(dependencies)
        }

        return modules
    }

    private val builtInsModuleInfo = BuiltInModuleInfo(Name.special("<built-ins>"))

    private fun createModule(moduleName: String): TestModuleInfo {
        parseModulePlatformByName(moduleName)
        return TestModuleInfo(Name.special("<$moduleName>"))
    }

    private class BuiltInModuleInfo(override val name: Name) : ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        override fun dependencies(): List<ModuleInfo> {
            return listOf(this)
        }
    }

    private class TestModuleInfo(override val name: Name) : ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        val dependencies = mutableListOf<ModuleInfo>(this)
        override fun dependencies(): List<ModuleInfo> {
            return dependencies
        }
    }

    private fun createSealedModule(): TestModuleInfo {
        return createModule("test-module-jvm").apply {
            dependencies += builtInsModuleInfo
        }
    }
}
