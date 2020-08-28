/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.checkers.DiagnosticDiffCallbacks
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.PositionalTextDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.SyntaxErrorDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.io.File
import java.util.*

abstract class AbstractFirBaseDiagnosticsTest : BaseDiagnosticsTest() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        try {
            analyzeAndCheckUnhandled(testDataFile, files, useLightTree)
        } catch (t: AssertionError) {
            throw t
        } catch (t: Throwable) {
            throw t
        }
    }

    protected open val useLightTree: Boolean
        get() = false

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        Extensions.getArea(environment.project)
            .getExtensionPoint(PsiElementFinder.EP_NAME)
            .unregisterExtension(JavaElementFinder::class.java)
    }

    open fun analyzeAndCheckUnhandled(testDataFile: File, files: List<TestFile>, useLightTree: Boolean = false) {
        val groupedByModule = files.groupBy(TestFile::module)

        val modules = createModules(groupedByModule)

        val sessionProvider = FirProjectSessionProvider(project)

        //For BuiltIns, registered in sessionProvider automatically
        val allProjectScope = GlobalSearchScope.allScope(project)

        FirSessionFactory.createLibrarySession(
            builtInsModuleInfo, sessionProvider, allProjectScope, project,
            environment.createPackagePartProvider(allProjectScope)
        )

        val configToSession = modules.mapValues { (config, info) ->
            val moduleFiles = groupedByModule.getValue(config)
            val scope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(
                project,
                moduleFiles.mapNotNull { it.ktFile })
            FirSessionFactory.createJavaModuleBasedSession(info, sessionProvider, scope) {
                configureSession()
                getFirExtensions()?.let {
                    registerExtensions(it)
                }
            }
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

        runAnalysis(testDataFile, files, firFilesPerSession)
    }

    protected open fun getFirExtensions(): BunchOfRegisteredExtensions? {
        return null
    }

    private fun mapKtFilesToFirFiles(session: FirSession, ktFiles: List<KtFile>, firFiles: MutableList<FirFile>, useLightTree: Boolean) {
        val firProvider = (session.firProvider as FirProviderImpl)
        if (useLightTree) {
            val lightTreeBuilder = LightTree2Fir(session, firProvider.kotlinScopeProvider, stubMode = false)
            ktFiles.mapTo(firFiles) {
                val firFile = lightTreeBuilder.buildFirFile(it.text, it.name)
                (session.firProvider as FirProviderImpl).recordFile(firFile)
                firFile
            }
        } else {
            val firBuilder = RawFirBuilder(session, firProvider.kotlinScopeProvider, false)
            ktFiles.mapTo(firFiles) {
                val firFile = firBuilder.buildFirFile(it)
                firProvider.recordFile(firFile)
                firFile
            }
        }
    }

    protected abstract fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>)

    private fun createModules(
        groupedByModule: Map<TestModule?, List<TestFile>>
    ): MutableMap<TestModule?, ModuleInfo> {
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
            //dependencies.addAll(getAdditionalDependencies(module))
            (module as TestModuleInfo).dependencies.addAll(dependencies)
        }

        return modules
    }

    private val builtInsModuleInfo = BuiltInModuleInfo(Name.special("<built-ins>"))

    protected open fun createModule(moduleName: String): TestModuleInfo {
        parseModulePlatformByName(moduleName)
        return TestModuleInfo(Name.special("<$moduleName>"))
    }

    class BuiltInModuleInfo(override val name: Name) :
        ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        override fun dependencies(): List<ModuleInfo> {
            return listOf(this)
        }
    }

    protected class TestModuleInfo(override val name: Name) :
        ModuleInfo {
        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices

        val dependencies = mutableListOf<ModuleInfo>(this)
        override fun dependencies(): List<ModuleInfo> {
            return dependencies
        }
    }

    protected open fun createSealedModule(): TestModuleInfo =
        createModule("test-module-jvm").apply {
            dependencies += builtInsModuleInfo
        }

    protected fun TestFile.getActualText(
        firDiagnostics: Iterable<FirDiagnostic<*>>,
        actualText: StringBuilder
    ): Boolean {
        val ktFile = this.ktFile
        if (ktFile == null) {
            // TODO: check java files too
            actualText.append(this.clearText)
            return true
        }

        if (ktFile.name.endsWith("CoroutineUtil.kt") && ktFile.packageFqName == FqName("helpers")) return true

        // TODO: report JVM signature diagnostics also for implementing modules

        val ok = booleanArrayOf(true)
        val diagnostics = firDiagnostics.toActualDiagnostic(ktFile)
        val filteredDiagnostics = diagnostics // TODO

        actualDiagnostics.addAll(filteredDiagnostics)

        val uncheckedDiagnostics = mutableListOf<PositionalTextDiagnostic>()

        val diagnosticToExpectedDiagnostic =
            CheckerTestUtil.diagnosticsDiff(
                diagnosedRanges,
                filteredDiagnostics,
                object : DiagnosticDiffCallbacks {
                    override fun missingDiagnostic(
                        diagnostic: TextDiagnostic,
                        expectedStart: Int,
                        expectedEnd: Int
                    ) {
                        val message =
                            "Missing " + diagnostic.description + PsiDiagnosticUtils.atLocation(
                                ktFile,
                                TextRange(
                                    expectedStart,
                                    expectedEnd
                                )
                            )
                        System.err.println(message)
                        ok[0] = false
                    }

                    override fun wrongParametersDiagnostic(
                        expectedDiagnostic: TextDiagnostic,
                        actualDiagnostic: TextDiagnostic,
                        start: Int,
                        end: Int
                    ) {
                        val message = "Parameters of diagnostic not equal at position " +
                                PsiDiagnosticUtils.atLocation(
                                    ktFile,
                                    TextRange(
                                        start,
                                        end
                                    )
                                ) +
                                ". Expected: ${expectedDiagnostic.asString()}, actual: $actualDiagnostic"
                        System.err.println(message)
                        ok[0] = false
                    }

                    override fun unexpectedDiagnostic(
                        diagnostic: TextDiagnostic,
                        actualStart: Int,
                        actualEnd: Int
                    ) {
                        val message =
                            "Unexpected ${diagnostic.description}${PsiDiagnosticUtils.atLocation(
                                ktFile,
                                TextRange(
                                    actualStart,
                                    actualEnd
                                )
                            )}"
                        System.err.println(message)
                        ok[0] = false
                    }

                    fun updateUncheckedDiagnostics(
                        diagnostic: TextDiagnostic,
                        start: Int,
                        end: Int
                    ) {
                        uncheckedDiagnostics.add(
                            PositionalTextDiagnostic(
                                diagnostic,
                                start,
                                end
                            )
                        )
                    }
                })

        actualText.append(
            CheckerTestUtil.addDiagnosticMarkersToText(
                ktFile,
                filteredDiagnostics,
                diagnosticToExpectedDiagnostic,
                { file -> file.text },
                uncheckedDiagnostics,
                false,
                false
            )
        )

        stripExtras(actualText)

        return ok[0]
    }

    private fun Iterable<FirDiagnostic<*>>.toActualDiagnostic(root: PsiElement): List<ActualDiagnostic> {
        val result = mutableListOf<ActualDiagnostic>()
        mapTo(result) {
            val oldDiagnostic = (it as FirPsiDiagnostic<*>).asPsiBasedDiagnostic()
            ActualDiagnostic(oldDiagnostic, null, true)
        }
        for (errorElement in AnalyzingUtils.getSyntaxErrorRanges(root)) {
            result.add(ActualDiagnostic(SyntaxErrorDiagnostic(errorElement), null, true))
        }
        return result
    }

    protected open fun FirSessionFactory.FirSessionConfigurator.configureSession() {}
}
