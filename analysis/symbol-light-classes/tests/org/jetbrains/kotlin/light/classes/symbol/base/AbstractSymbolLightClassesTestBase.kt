/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.light.classes.symbol.base.service.NullabilityAnnotationSourceProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.test.fail

// Same as LightProjectDescriptor.TEST_MODULE_NAME
private const val TEST_MODULE_NAME = "light_idea_test_case"

abstract class AbstractSymbolLightClassesTestBase(
    override val configurator: AnalysisApiTestConfigurator,
) : AbstractAnalysisApiBasedTest() {

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useAdditionalServices(service(::CompiledLibraryProvider))
            useDirectives(Directives, TestModuleCompiler.Directives)
            useAdditionalSourceProviders(::NullabilityAnnotationSourceProvider)
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
                ModuleStructureDirectives.MODULE + TEST_MODULE_NAME
            }
        }
    }

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: TestModule, testServices: TestServices) {
        val ktFiles = testServices.ktModuleProvider.getKtFiles(mainModule)
        doLightClassTest(ktFiles, mainModule, testServices)
    }

    open fun doLightClassTest(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        if (isTestAgainstCompiledCode && TestModuleCompiler.Directives.COMPILATION_ERRORS in module.directives) {
            return
        }

        val ktFile = ktFiles.first()
        val project = ktFile.project

        ignoreExceptionIfIgnoreDirectivePresent(module) {
            compareResults(module, testServices) {
                getRenderResult(ktFile, ktFiles, testDataPath, module, project)
            }
        }
    }

    protected fun compareResults(module: TestModule, testServices: TestServices, computeActual: () -> String) {
        val actual = computeActual().cleanup()
        compareResults(testServices, actual)
        removeIgnoreDirectives(module)
        removeDuplicatedFirJava(testServices)
    }

    private fun String.cleanup(): String {
        val lines = this.lines().mapTo(mutableListOf()) { it.ifBlank { "" } }
        if (lines.last().isNotBlank()) {
            lines += ""
        }
        return lines.joinToString("\n")
    }

    protected abstract fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: TestModule,
        project: Project,
    ): String

    protected fun ignoreExceptionIfIgnoreDirectivePresent(module: TestModule, action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            val directives = module.directives
            if (Directives.IGNORE_FIR in directives || isTestAgainstCompiledCode && Directives.IGNORE_LIBRARY_EXCEPTIONS in directives) {
                return
            }

            throw e
        }
    }

    private fun compareResults(
        testServices: TestServices,
        actual: String,
    ) {
        val path: Path = currentResultPath().takeIf { it.exists() } ?: javaPath()
        testServices.assertions.assertEqualsToFile(path, actual)
    }

    private fun removeIgnoreDirectives(module: TestModule) {
        val directives = module.directives
        if (Directives.IGNORE_FIR in directives) {
            throwTestIsPassingException(Directives.IGNORE_FIR)
        }

        if (isTestAgainstCompiledCode && Directives.IGNORE_LIBRARY_EXCEPTIONS in directives) {
            throwTestIsPassingException(Directives.IGNORE_LIBRARY_EXCEPTIONS)
        }
    }

    private fun throwTestIsPassingException(directive: Directive): Nothing {
        error("Test is passing. Please, remove `// ${directive.name}` directive")
    }

    protected fun findLightClass(fqname: String, ktFile: KtFile): PsiClass? {
        val project = ktFile.project
        return findLightClass(fqname, GlobalSearchScope.fileScope(ktFile), project) ?: findLightClass(fqname, project)
    }

    protected fun findLightClass(fqname: String, project: Project): PsiClass? {
        return findLightClass(fqname, GlobalSearchScope.allScope(project), project)
    }

    private fun findLightClass(fqname: String, scope: GlobalSearchScope, project: Project): PsiClass? {
        JavaElementFinder.getInstance(project).findClass(fqname, scope)?.let { return it }

        val fqName = FqName(fqname)
        val parentFqName = fqName.parent().takeUnless(FqName::isRoot) ?: return null
        val enumClass = JavaElementFinder.getInstance(project).findClass(parentFqName.asString(), scope) ?: return null
        val kotlinEnumClass = enumClass.unwrapped?.safeAs<KtClass>()?.takeIf(KtClass::isEnum) ?: return null

        val enumEntryName = fqName.shortName().asString()
        enumClass.findInnerClassByName(enumEntryName, false)?.let { return it }

        return kotlinEnumClass.declarations.firstNotNullOfOrNull {
            if (it is KtEnumEntry && it.name == enumEntryName) {
                it
            } else {
                null
            }
        }?.toLightClass()
    }

    private fun removeDuplicatedFirJava(testServices: TestServices) {
        val java = javaPath()
        val firJava = currentResultPath()
        if (!firJava.exists()) return
        val identicalCheckerHelper = IdenticalCheckerHelper(testServices)
        if (identicalCheckerHelper.contentsAreEquals(java.toFile(), firJava.toFile(), trimLines = true)) {
            identicalCheckerHelper.deleteFirFile(java.toFile())
            fail("$firJava is equals to $java. The redundant test data file removed")
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
    protected abstract val isTestAgainstCompiledCode: Boolean

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

        val IGNORE_LIBRARY_EXCEPTIONS by stringDirective(
            description = "Ignore the test for decompiled-based implementation of LC"
        )
    }
}
