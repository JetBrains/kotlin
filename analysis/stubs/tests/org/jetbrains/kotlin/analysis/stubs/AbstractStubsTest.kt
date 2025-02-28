/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.util.indexing.FileContentImpl
import junit.framework.TestCase.assertEquals
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractStubsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val files = mainModule.ktFiles
        val filesAndStubs = files.map { it to computeStub(it) }

        val actual = prettyPrint {
            if (filesAndStubs.isEmpty()) {
                appendLine("NO FILES")
                return@prettyPrint
            }

            val singleElement = filesAndStubs.singleOrNull()
            if (singleElement != null) {
                renderStub(singleElement.second)
            } else {
                printCollection(filesAndStubs, separator = "\n\n") { element ->
                    appendLine("${element.first.name}:")
                    withIndent {
                        renderStub(element.second)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".stubs.txt")

        for ((_, stub) in filesAndStubs) {
            checkPsiElementTypeConsistency(testServices.assertions, stub ?: continue)
        }
    }

    private fun checkPsiElementTypeConsistency(assertions: AssertionsService, stubElement: StubElement<*>) {
        val psi = stubElement.psi as? StubBasedPsiElement<*>
        if (psi != null) {
            assertions.assertEquals(
                stubElement.stubType,
                psi.elementType,
                { "Expected the PSI of `$stubElement` to have the same element type. Instead got: `${psi.elementType}`." },
            )
        }

        stubElement.childrenStubs.forEach {
            checkPsiElementTypeConsistency(assertions, it)
        }
    }

    private fun PrettyPrinter.renderStub(stub: PsiFileStub<*>?) {
        val treeStr = stub?.serializeToString()
        append(treeStr)
    }

    abstract fun computeStub(file: KtFile): PsiFileStub<*>?
}

abstract class AbstractSourceStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun computeStub(file: KtFile): PsiFileStub<*>? = KtFileStubBuilder().buildStubTree(file) as PsiFileStub<*>
}

abstract class AbstractCompiledStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator()

    override fun computeStub(file: KtFile): PsiFileStub<*>? = ClsClassFinder.allowMultifileClassPart {
        KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(file.virtualFile))
    }
}

private open class CompiledStubsTestConfigurator : AnalysisApiFirBinaryTestConfigurator() {
    override val testModuleFactory: KtTestModuleFactory get() = KtLibraryBinaryDecompiledTestModuleFactory
    override val testPrefixes: List<String> get() = listOf("compiled")
}

abstract class AbstractDecompiledStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = object : CompiledStubsTestConfigurator() {
        override val testPrefixes: List<String> get() = listOf("decompiled") + super.testPrefixes
    }

    override fun computeStub(file: KtFile): PsiFileStub<*>? = KtFileStubBuilder().buildStubTree(file) as PsiFileStub<*>
}
