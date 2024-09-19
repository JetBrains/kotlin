/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.impl.PsiManagerEx
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.decompiled.light.classes.fe10.KotlinDeclarationInCompiledFileSearcherFE10Impl
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractBlubTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator =
        object : StandaloneModeBinaryTestConfigurator() {
            override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
                get() = super.serviceRegistrars + object : AnalysisApiTestServiceRegistrar() {
                    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
                        application.registerService(
                            KotlinDeclarationInCompiledFileSearcher::class.java,
                            KotlinDeclarationInCompiledFileSearcherFE10Impl()
                        )
                    }

                    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
                        project.registerService(PsiManagerEx::class.java)

                        testServices.environmentManager.getApplication()
                    }
                }

            override val testModuleFactory: KtTestModuleFactory
                get() = KtLibraryBinaryDecompiledTestModuleFactory
        }

    /*override fun setUp() {
        super.setUp()

        with(environment.projectEnvironment.project) {
            registerService(ClsJavaStubByVirtualFileCache::class.java, ClsJavaStubByVirtualFileCache())
            registerService(KotlinAsJavaSupport::class.java, SymbolKotlinAsJavaSupport::class.java)
            registerService(
                KotlinProjectStructureProvider::class.java,
                KotlinTestProjectStructureProvider(globalLanguageVersionSettings, builtinsModule, ktTestModuleStructure)
            )
        }

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, testRootDisposable)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, testRootDisposable)
        }
    }*/

    override fun doTest(testServices: TestServices) {
        testServices.assertions.assertEqualsToTestDataFileSibling(extractFacades(testServices))
    }

    /*fun getClassFileToDecompile(testServices: TestServices): List<VirtualFile> {
        val extraOptions = buildList {
            this.add(org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments::allowKotlinPackage.cliArgument)
            addAll(listOf(org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments::languageVersion.cliArgument, getLanguageVersionToCompile().versionString))
        }
        val library = org.jetbrains.kotlin.test.CompilerTestUtil.compileJvmLibrary(
            src = java.io.File(testServices.ktTestModuleStructure.allMainKtFiles.single().virtualFilePath),
            extraOptions = extraOptions,
        ).toPath()

        val jarFileSystem = testServices.environmentManager.getProjectEnvironment().environment.jarFileSystem as CoreJarFileSystem
        val root = jarFileSystem.refreshAndFindFileByPath(library.absolutePathString() + "!/")!!

        return buildList {
            com.intellij.openapi.vfs.VfsUtilCore.iterateChildrenRecursively(
                root,
                /*filter=*/{ virtualFile ->
                    virtualFile.isDirectory || virtualFile.name.endsWith(".class")
                },
                /*iterator=*/{ virtualFile ->
                    if (!virtualFile.isDirectory) {
                        addIfNotNull(virtualFile)
                    }
                    true
                })
        }
    }*/

    private fun extractFacades(testServices: TestServices): String {

        val project = testServices.environmentManager.getProject()
        val classFiles = testServices.ktTestModuleStructure.allMainKtFiles

        return prettyPrint {
            classFiles.sortedBy { it.virtualFilePath }.forEach { classFile ->
                val path = classFile.virtualFilePath.substringAfterLast(".jar!/")
                appendLine("file: $path")
                val facade = KotlinAsJavaSupport.getInstance(project).getLightFacade(classFile)!!

                appendFacade(facade)
            }
        }
    }

    private fun PrettyPrinter.appendFacade(psi: PsiElement) {
        when (psi) {
            is KtLightElement<*, *> -> {
                appendLine("- $psi (origin: ${psi.kotlinOrigin?.toOriginString()})")
            }
            is PsiAnnotation -> {
                appendLine("- ${psi.nameReferenceElement?.text}")
            }
            else -> {
                appendLine("- $psi")
            }
        }

        fun extractAll(name: String, elements: Iterator<PsiElement>) {
            val it = elements.iterator()
            if (it.hasNext()) {
                withIndent {
                    appendLine("$name:")
                    it.forEach { appendFacade(it) }
                }
            }
        }

        fun extractAll(name: String, elements: Array<out PsiElement>?) {
            elements?.iterator()?.also { extractAll(name, it) }
        }

        // visit children
        when(psi) {
            is PsiClass -> {
                extractAll("annotations", psi.annotations)
                extractAll("implements", psi.implementsList?.referenceElements)
                extractAll("extends", psi.extendsList?.referenceElements)
                extractAll("fields", psi.fields)
                extractAll("methods", psi.methods)
                extractAll("innerClass", psi.innerClasses)
            }
            is PsiMethod -> {
                extractAll("annotations", psi.annotations)
                extractAll("parameters", psi.parameterList.parameters)
            }
            is PsiField -> {
                extractAll("annotations", psi.annotations)
            }
            is PsiParameter -> {
                extractAll("annotations", psi.annotations)
            }
            is PsiAnnotation -> {
                // nothing
            }
            else -> error("Should have handled: $psi")
        }
    }

    private fun KtElement.toOriginString(): String =
        "$this ${this.name} in ${this.containingKtFile.virtualFile.name}"
}