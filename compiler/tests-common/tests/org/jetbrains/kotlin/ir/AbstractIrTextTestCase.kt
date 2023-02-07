/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.js.loadPluginsForTests
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.util.regex.Pattern

abstract class AbstractIrTextTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val irModule = buildFragmentAndTestIt(wholeFile, testFiles)
        doTestIrModuleDependencies(wholeFile, irModule)
    }

    protected fun buildFragmentAndTestIt(wholeFile: File, testFiles: List<TestFile>): IrModuleFragment {
        val dir = wholeFile.parentFile
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        val irModule = generateIrModule(ignoreErrors)

        val ktFiles = testFiles.filter { it.name.endsWith(".kt") || it.name.endsWith(".kts") }
        for ((testFile, irFile) in ktFiles.zip(irModule.files)) {
            doTestIrFileAgainstExpectations(dir, testFile, irFile)
        }

        return irModule
    }

    private fun doTestIrModuleDependencies(wholeFile: File, irModule: IrModuleFragment) {
        val wholeText = wholeFile.readText()
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)

        val stubGenerator = DeclarationStubGeneratorImpl(
            irModule.descriptor,
            SymbolTable(signaturer, IrFactoryImpl), // TODO
            irModule.irBuiltins,
            DescriptorByIdSignatureFinderImpl(irModule.descriptor, mangler)
        )

        val path = wholeFile.path
        val replacedPath = path.replace(".kt", "__")
        val filesInDir = wholeFile.parentFile.listFiles() ?: return
        val externalFilePaths = filesInDir.mapNotNullTo(mutableListOf()) {
            if (it.path.startsWith(replacedPath)) it.path else null
        }
        for (externalClassFqn in parseDumpExternalClasses(wholeText)) {
            val classDump = stubGenerator.generateExternalClass(irModule.descriptor, externalClassFqn).dump()
            val expectedFilePath = path.replace(".kt", "__$externalClassFqn.txt")
            val expectedFile = File(expectedFilePath)
            externalFilePaths -= expectedFilePath
            KotlinTestUtils.assertEqualsToFile(expectedFile, classDump)
        }
        KtUsefulTestCase.assertEmpty("The following external dump files were not built: $externalFilePaths", externalFilePaths)
    }

    private fun DeclarationStubGenerator.generateExternalClass(descriptor: ModuleDescriptor, externalClassFqn: String): IrClass {
        val classDescriptor =
            descriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(externalClassFqn)))
                ?: throw AssertionError("Can't find a class in external dependencies: $externalClassFqn")

        return generateMemberStub(classDescriptor) as IrClass
    }

    override fun configureTestSpecific(configuration: CompilerConfiguration, testFiles: List<TestFile>) {
        if (testFiles.any { it.name.endsWith(".kts") }) {
            loadPluginsForTests(configuration)
        }
    }

    private fun doTestIrFileAgainstExpectations(dir: File, testFile: TestFile, irFile: IrFile) {
        if (testFile.isExternalFile()) return

        val expectations = parseExpectations(dir, testFile)
        val irFileDump = irFile.dump(normalizeNames = true)

        val expected = StringBuilder()
        val actual = StringBuilder()
        for (expectation in expectations.regexps) {
            expected.append(expectation.numberOfOccurrences).append(" ").append(expectation.needle).append("\n")
            val actualCount = StringUtil.findMatches(irFileDump, Pattern.compile("(" + expectation.needle + ")")).size
            actual.append(actualCount).append(" ").append(expectation.needle).append("\n")
        }

        for (irTreeFileLabel in expectations.irTreeFileLabels) {
            val actualTrees = irFile.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber, normalizeNames = true)
            KotlinTestUtils.assertEqualsToFile(irTreeFileLabel.expectedTextFile, actualTrees)
            verify(irFile)

            // Check that deep copy produces an equivalent result
            val irFileCopy = irFile.deepCopyWithSymbols()
            val copiedTrees = irFileCopy.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber, normalizeNames = true)
            TestCase.assertEquals("IR dump mismatch after deep copy with symbols", actualTrees, copiedTrees)
            verify(irFileCopy)

            if (!testFile.directives.contains("SKIP_KT_DUMP")) {
                val kotlinLikeDump = irFile.dumpKotlinLike(
                    KotlinLikeDumpOptions(
                        printFileName = false,
                        printFilePath = false,
                        printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                    )
                )
                val kotlinLikeDumpExpectedFile = irTreeFileLabel.expectedTextFile.withReplacedExtensionOrNull(".txt", ".kt.txt")!!
                KotlinTestUtils.assertEqualsToFile(kotlinLikeDumpExpectedFile, kotlinLikeDump)
            }
        }

        try {
            TestCase.assertEquals(irFileDump, expected.toString(), actual.toString())
        } catch (e: Throwable) {
            println(irFileDump)
            throw rethrow(e)
        }
    }

    private fun verify(irFile: IrFile) {
        IrVerifier(JUnit4Assertions, isFir = false).verifyWithAssert(irFile)
    }

    internal class Expectations(val regexps: List<RegexpInText>, val irTreeFileLabels: List<IrTreeFileLabel>)

    internal class RegexpInText(val numberOfOccurrences: Int, val needle: String) {
        constructor(countStr: String, needle: String) : this(Integer.valueOf(countStr), needle)
    }

    internal class IrTreeFileLabel(val expectedTextFile: File, val lineNumber: Int)

    protected open fun getExpectedTextFileName(testFile: TestFile, name: String = testFile.name): String {
        return name.replace(".kts", ".txt").replace(".kt", ".txt")
    }

    private fun parseExpectations(dir: File, testFile: TestFile): Expectations {
        val regexps =
            testFile.content.matchLinesWith(EXPECTED_OCCURRENCES_PATTERN) {
                RegexpInText(it.groupValues[1], it.groupValues[2].trim())
            }

        var treeFiles =
            testFile.content.matchLinesWith(IR_FILE_TXT_PATTERN) {
                val fileName = it.groupValues[1].trim()
                val file = File(dir, getExpectedTextFileName(testFile, fileName))
                IrTreeFileLabel(file, 0)
            }

        if (treeFiles.isEmpty()) {
            val file = File(dir, getExpectedTextFileName(testFile))
            treeFiles = listOf(IrTreeFileLabel(file, 0))
        }

        return Expectations(regexps, treeFiles)
    }

    companion object {
        private val EXPECTED_OCCURRENCES_PATTERN = Regex("""^\s*//\s*(\d+)\s*(.*)$""")
        private val IR_FILE_TXT_PATTERN = Regex("""// IR_FILE: (.*)$""")

        private val DUMP_EXTERNAL_CLASS = Regex("""// DUMP_EXTERNAL_CLASS (.*)""")

        private val EXTERNAL_FILE_PATTERN = Regex("""// EXTERNAL_FILE""")

        private inline fun <T> String.matchLinesWith(regex: Regex, ifMatched: (MatchResult) -> T): List<T> =
            lines().mapNotNull { regex.matchEntire(it)?.let(ifMatched) }

        internal fun parseDumpExternalClasses(text: String) =
            text.matchLinesWith(DUMP_EXTERNAL_CLASS) { it.groupValues[1] }

        internal fun TestFile.isExternalFile() =
            EXTERNAL_FILE_PATTERN.containsMatchIn(content)
    }

}
