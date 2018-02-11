/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.util.regex.Pattern

abstract class AbstractIrTextTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val dir = wholeFile.parentFile
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        val irModule = generateIrModule(ignoreErrors)

        val ktFiles = testFiles.filter { it.name.endsWith(".kt") }
        for ((testFile, irFile) in ktFiles.zip(irModule.files)) {
            doTestIrFileAgainstExpectations(dir, testFile, irFile)
        }

        if (shouldDumpDependencies(wholeFile)) {
            doTestIrModuleDependencies(wholeFile, irModule)
        }
    }

    private fun doTestIrModuleDependencies(wholeFile: File, irModule: IrModuleFragment) {
        irModule.dependencyModules.forEach { irDependencyModule ->
            val actual = irDependencyModule.dump()
            val sanitizedModuleName = StringUtil.sanitizeJavaIdentifier(irDependencyModule.descriptor.name.asString())
            val expectedFileName = wholeFile.absolutePath.replace(".kt", "__$sanitizedModuleName.txt")
            KotlinTestUtils.assertEqualsToFile(File(expectedFileName), actual)
        }
    }

    protected fun doTestIrFileAgainstExpectations(dir: File, testFile: TestFile, irFile: IrFile) {
        if (testFile.isExternalFile()) return

        val expectations = parseExpectations(dir, testFile)
        val irFileDump = irFile.dump()

        val expected = StringBuilder()
        val actual = StringBuilder()
        for (expectation in expectations.regexps) {
            expected.append(expectation.numberOfOccurrences).append(" ").append(expectation.needle).append("\n")
            val actualCount = StringUtil.findMatches(irFileDump, Pattern.compile("(" + expectation.needle + ")")).size
            actual.append(actualCount).append(" ").append(expectation.needle).append("\n")
        }

        for (irTreeFileLabel in expectations.irTreeFileLabels) {
            val actualTrees = irFile.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber)
            KotlinTestUtils.assertEqualsToFile(irTreeFileLabel.expectedTextFile, actualTrees)
            verify(irFile)

            // Check that deep copy produces an equivalent result
            val irFileCopy = irFile.deepCopyWithSymbols()
            val copiedTrees = irFileCopy.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber)
            TestCase.assertEquals("IR dump mismatch after deep copy with symbols", actualTrees, copiedTrees)
            verify(irFileCopy)

            val irFileCopyOld = irFile.deepCopyOld()
            val copiedTreesOld = irFileCopyOld.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber)
            TestCase.assertEquals("IR dump mismatch after old deep copy", actualTrees, copiedTreesOld)
        }

        try {
            TestCase.assertEquals(irFileDump, expected.toString(), actual.toString())
        } catch (e: Throwable) {
            println(irFileDump)
            throw rethrow(e)
        }
    }

    private fun verify(irFile: IrFile) {
        IrVerifier().verifyWithAssert(irFile)
    }

    private class IrVerifier : IrElementVisitorVoid {
        private val errors = ArrayList<String>()

        private val symbolForDeclaration = HashMap<IrElement, IrSymbol>()

        val hasErrors get() = errors.isNotEmpty()

        val errorsAsMessage get() = errors.joinToString(prefix = "IR verifier errors:\n", separator = "\n")

        private fun error(message: String) {
            errors.add(message)
        }

        fun verifyWithAssert(irFile: IrFile) {
            irFile.acceptChildrenVoid(this)
            TestCase.assertFalse(errorsAsMessage + "\n\n\n" + irFile.dump(), hasErrors)
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitDeclaration(declaration: IrDeclaration) {
            if (declaration is IrSymbolOwner) {
                declaration.symbol.checkBinding("decl", declaration)

                if (declaration.symbol.owner != declaration) {
                    error("Symbol is not bound to declaration: ${declaration.render()}")
                }
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            visitDeclaration(declaration)

            val functionDescriptor = declaration.descriptor

            checkTypeParameters(functionDescriptor, declaration, functionDescriptor.typeParameters)

            val expectedDispatchReceiver = functionDescriptor.dispatchReceiverParameter
            val actualDispatchReceiver = declaration.dispatchReceiverParameter?.descriptor
            if (expectedDispatchReceiver != actualDispatchReceiver) {
                error(
                    "$functionDescriptor: Dispatch receiver parameter mismatch: " +
                            "expected $expectedDispatchReceiver, actual $actualDispatchReceiver"
                )
            }

            val expectedExtensionReceiver = functionDescriptor.extensionReceiverParameter
            val actualExtensionReceiver = declaration.extensionReceiverParameter?.descriptor
            if (expectedExtensionReceiver != actualExtensionReceiver) {
                error(
                    "$functionDescriptor: Extension receiver parameter mismatch: " +
                            "expected $expectedExtensionReceiver, actual $actualExtensionReceiver"
                )
            }

            val declaredValueParameters = declaration.valueParameters.map { it.descriptor }
            val actualValueParameters = functionDescriptor.valueParameters
            if (declaredValueParameters.size != actualValueParameters.size) {
                error("$functionDescriptor: Value parameters mismatch: $declaredValueParameters != $actualValueParameters")
            } else {
                declaredValueParameters.zip(actualValueParameters).forEach { (declaredValueParameter, actualValueParameter) ->
                    if (declaredValueParameter != actualValueParameter) {
                        error("$functionDescriptor: Value parameters mismatch: $declaredValueParameter != $actualValueParameter")
                    }
                }
            }
        }

        override fun visitDeclarationReference(expression: IrDeclarationReference) {
            expression.symbol.checkBinding("ref", expression)
        }

        override fun visitFunctionReference(expression: IrFunctionReference) {
            expression.symbol.checkBinding("ref", expression)
        }

        override fun visitPropertyReference(expression: IrPropertyReference) {
            expression.field?.checkBinding("field", expression)
            expression.getter?.checkBinding("getter", expression)
            expression.setter?.checkBinding("setter", expression)
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
            expression.delegate.checkBinding("delegate", expression)
            expression.getter.checkBinding("getter", expression)
            expression.setter?.checkBinding("setter", expression)
        }

        private fun IrSymbol.checkBinding(kind: String, irElement: IrElement) {
            if (!isBound) {
                error("${javaClass.simpleName} $descriptor is unbound @$kind ${irElement.render()}")
            }

            val otherSymbol = symbolForDeclaration.getOrPut(owner) { this }
            if (this != otherSymbol) {
                error("Multiple symbols for $descriptor @$kind ${irElement.render()}")
            }
        }

        override fun visitClass(declaration: IrClass) {
            visitDeclaration(declaration)

            checkTypeParameters(declaration.descriptor, declaration, declaration.descriptor.declaredTypeParameters)
        }

        private fun checkTypeParameters(
            descriptor: DeclarationDescriptor,
            declaration: IrTypeParametersContainer,
            expectedTypeParameters: List<TypeParameterDescriptor>
        ) {
            val declaredTypeParameters = declaration.typeParameters.map { it.descriptor }

            if (declaredTypeParameters.size != expectedTypeParameters.size) {
                error("$descriptor: Type parameters mismatch: $declaredTypeParameters != $expectedTypeParameters")
            } else {
                declaredTypeParameters.zip(expectedTypeParameters).forEach { (declaredTypeParameter, expectedTypeParameter) ->
                    if (declaredTypeParameter != expectedTypeParameter) {
                        error("$descriptor: Type parameters mismatch: $declaredTypeParameter != $expectedTypeParameter")
                    }
                }
            }
        }
    }

    internal class Expectations(val regexps: List<RegexpInText>, val irTreeFileLabels: List<IrTreeFileLabel>)

    internal class RegexpInText(val numberOfOccurrences: Int, val needle: String) {
        constructor(countStr: String, needle: String) : this(Integer.valueOf(countStr), needle)
    }

    internal class IrTreeFileLabel(val expectedTextFile: File, val lineNumber: Int)

    companion object {
        private val EXPECTED_OCCURRENCES_PATTERN = Regex("""^\s*//\s*(\d+)\s*(.*)$""")
        private val IR_FILE_TXT_PATTERN = Regex("""// IR_FILE: (.*)$""")

        private val DUMP_DEPENDENCIES_PATTERN = Regex("""// !DUMP_DEPENDENCIES""")

        private val EXTERNAL_FILE_PATTERN = Regex("""// EXTERNAL_FILE""")

        internal fun shouldDumpDependencies(wholeFile: File): Boolean =
            DUMP_DEPENDENCIES_PATTERN.containsMatchIn(wholeFile.readText())

        internal fun TestFile.isExternalFile() =
            EXTERNAL_FILE_PATTERN.containsMatchIn(content)

        internal fun KtFile.isExternalFile() =
            EXTERNAL_FILE_PATTERN.containsMatchIn(text)

        internal fun parseExpectations(dir: File, testFile: TestFile): Expectations {
            val regexps = ArrayList<RegexpInText>()
            val treeFiles = ArrayList<IrTreeFileLabel>()

            for (line in testFile.content.split("\n")) {
                EXPECTED_OCCURRENCES_PATTERN.matchEntire(line)?.let { matchResult ->
                    regexps.add(RegexpInText(matchResult.groupValues[1], matchResult.groupValues[2].trim()))
                } ?: IR_FILE_TXT_PATTERN.find(line)?.let { matchResult ->
                    val fileName = matchResult.groupValues[1].trim()
                    val file = createExpectedTextFile(testFile, dir, fileName)
                    treeFiles.add(IrTreeFileLabel(file, 0))
                }
            }

            if (treeFiles.isEmpty()) {
                val file = createExpectedTextFile(testFile, dir, testFile.name.replace(".kt", ".txt"))
                treeFiles.add(IrTreeFileLabel(file, 0))
            }

            return Expectations(regexps, treeFiles)
        }
    }

}
