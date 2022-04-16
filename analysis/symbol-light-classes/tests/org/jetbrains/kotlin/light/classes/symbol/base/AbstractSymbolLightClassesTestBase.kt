/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.pico.DefaultPicoContainer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompilerExecutor
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.light.classes.symbol.base.service.NullabilityAnnotationSourceProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

abstract class AbstractSymbolLightClassesTestBase(
    override val configurator: AnalysisApiTestConfigurator
) : AbstractAnalysisApiBasedSingleModuleTest() {

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useAdditionalServices(service(::CompiledLibraryProvider))
            useDirectives(Directives, CompilerExecutor.Directives)
            useAdditionalSourceProviders(::NullabilityAnnotationSourceProvider)
        }
    }

    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        if (stopIfCompilationErrorDirectivePresent && CompilerExecutor.Directives.COMPILATION_ERRORS in module.directives) {
            return
        }
        val testDataFile = module.files.first { it.name.endsWith(".kt") }.originalFile.toPath()

        val ktFile = ktFiles.first()
        val project = ktFile.project

        ignoreExceptionIfIgnoreFirPresent(module) {
            val actual = getRenderResult(ktFile, testDataFile, module, project)
            compareResults(testServices, actual)
            removeIgnoreFir(module)
            removeDuplicatedFirJava(testServices)
        }
    }

    protected abstract fun getRenderResult(
        ktFile: KtFile,
        testDataFile: Path,
        module: TestModule,
        project: Project
    ): String

    private inline fun ignoreExceptionIfIgnoreFirPresent(module: TestModule, action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            if (Directives.IGNORE_FIR in module.directives) {
                return
            }
            throw e
        }
    }

    private fun compareResults(
        testServices: TestServices,
        actual: String,
    ) {
        if (currentResultPath().exists()) {
            testServices.assertions.assertEqualsToFile(currentResultPath(), actual)
        } else {
            testServices.assertions.assertEqualsToFile(javaPath(), actual)
        }
    }

    private fun removeIgnoreFir(module: TestModule) {
        if (Directives.IGNORE_FIR in module.directives) {
            error("Test is passing. Please, remove `// ${Directives.IGNORE_FIR.name}` directive")
        }
    }

    private fun findLightClass(fqname: String, project: Project): PsiClass? {
        return JavaElementFinder
            .getInstance(project)
            .findClass(fqname, GlobalSearchScope.allScope(project))
    }

    private fun removeDuplicatedFirJava(testServices: TestServices) {
        val java = javaPath()
        val firJava = currentResultPath()
        if (!firJava.exists()) return
        val identicalCheckerHelper = IdenticalCheckerHelper(testServices)
        if (identicalCheckerHelper.contentsAreEquals(java.toFile(), firJava.toFile(), trimLines = true)) {
            identicalCheckerHelper.deleteFirFile(java.toFile())
        }
    }

    private inner class IdenticalCheckerHelper(testServices: TestServices) : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File {
            return if (testDataFile.name.endsWith(EXTENSIONS.FIR_JAVA))
                testDataFile.resolveSibling(testDataFile.name.removeSuffix(currentExtension) + EXTENSIONS.JAVA)
            else testDataFile
        }

        override fun getFirFileToCompare(testDataFile: File): File {
            return if (testDataFile.name.endsWith(EXTENSIONS.FIR_JAVA))
                testDataFile
            else testDataFile.resolveSibling(testDataFile.name.removeSuffix(EXTENSIONS.JAVA) + currentExtension)
        }
    }

    private fun javaPath() = testDataPath.resolveSibling(testDataPath.nameWithoutExtension + EXTENSIONS.JAVA)
    private fun currentResultPath() = testDataPath.resolveSibling(testDataPath.nameWithoutExtension + currentExtension)

    protected abstract val currentExtension: String
    protected abstract val stopIfCompilationErrorDirectivePresent: Boolean

    object EXTENSIONS {
        const val JAVA = ".java"
        const val FIR_JAVA = ".fir.java"
        const val LIB_JAVA = ".lib.java"
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_FIR by directive(
            description = "Ignore the test for Symbol FIR-based implementation of LC",
            applicability = DirectiveApplicability.Global
        )
    }
}