/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.light.classes.symbol.base.service.NullabilityAnnotationSourceProvider
import org.jetbrains.kotlin.light.classes.symbol.withMultiplatformLightClassSupport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.nio.file.Path

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
            if (configurator.defaultTargetPlatform.has<JvmPlatform>()) {
                useAdditionalSourceProviders(::NullabilityAnnotationSourceProvider)
            }
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
                ModuleStructureDirectives.MODULE + TEST_MODULE_NAME
            }
        }
    }

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val ktFiles = mainModule.ktFiles
        doLightClassTest(ktFiles, mainModule, testServices)
    }

    @OptIn(KaNonPublicApi::class)
    open fun doLightClassTest(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices) {
        if (isTestAgainstCompiledCode && TestModuleCompiler.Directives.COMPILATION_ERRORS in module.testModule.directives) {
            return
        }

        val ktFile = ktFiles.first()
        val project = ktFile.project

        ignoreExceptionIfIgnoreDirectivePresent(module) {
            compareResults(module, testServices) {
                if (configurator.defaultTargetPlatform.has<JvmPlatform>()) {
                    getRenderResult(ktFile, ktFiles, testDataPath, module, project)
                } else {
                    withMultiplatformLightClassSupport(project) {
                        getRenderResult(ktFile, ktFiles, testDataPath, module, project)
                    }
                }
            }
        }
    }

    protected fun compareResults(module: KtTestModule, testServices: TestServices, computeActual: () -> String) {
        val actual = computeActual().cleanup()

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".java")
        removeIgnoreDirectives(module)
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
        module: KtTestModule,
        project: Project,
    ): String

    protected fun ignoreExceptionIfIgnoreDirectivePresent(module: KtTestModule, action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            val directives = module.testModule.directives
            if (Directives.IGNORE_FIR in directives || isTestAgainstCompiledCode && Directives.IGNORE_LIBRARY_EXCEPTIONS in directives) {
                return
            }

            throw e
        }
    }

    private fun removeIgnoreDirectives(module: KtTestModule) {
        val directives = module.testModule.directives
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
        return findLightClass(fqname, GlobalSearchScope.fileScope(ktFile), project)
            ?: findLightClass(fqname, project)
            // TODO: KT-78534 JavaElementFinder: support script search
            ?: ktFile.script?.takeIf { it.fqName.asString() == fqname }?.toLightClass()
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

    protected abstract val isTestAgainstCompiledCode: Boolean

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
