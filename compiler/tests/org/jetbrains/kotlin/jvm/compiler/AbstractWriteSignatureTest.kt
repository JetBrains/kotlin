/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jvm.compiler

import com.google.common.io.Closeables
import com.google.common.io.Files
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.*
import org.junit.Assert
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.*
import java.util.regex.MatchResult

abstract class AbstractWriteSignatureTest : TestCaseWithTmpdir() {
    private var environment: KotlinCoreEnvironment? = null

    override fun setUp() {
        super.setUp()
        environment = KotlinTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                myTestRootDisposable, ConfigurationKind.ALL, jdkKind
        )
    }

    protected open val jdkKind: TestJdkKind
        get() = TestJdkKind.MOCK_JDK

    override fun tearDown() {
        environment = null
        super.tearDown()
    }

    protected fun doTest(ktFileName: String) {
        val ktFile = File(ktFileName)
        val text = FileUtil.loadFile(ktFile, true)

        val psiFile = KotlinTestUtils.createFile(ktFile.name, text, environment!!.project)

        val fileFactory = GenerationUtils.compileFileTo(psiFile, environment!!, tmpdir)

        Disposer.dispose(myTestRootDisposable)

        val expectations = parseExpectations(ktFile)
        try {
            expectations.check()
        }
        catch (e: Throwable) {
            println(fileFactory.createText())
            throw e
        }
    }

    private class SignatureExpectation(val header: String, val name: String, val expectedJvmSignature: String?, expectedGenericSignature: String) {
        private val expectedFormattedSignature = formatSignature(header, expectedJvmSignature, expectedGenericSignature)
        private val jvmDescriptorToFormattedSignature = mutableMapOf<String, String>()

        fun accept(name: String, actualJvmSignature: String, actualGenericSignature: String) {
            if (this.name == name) {
                Assert.assertFalse(jvmDescriptorToFormattedSignature.containsKey(actualJvmSignature))

                jvmDescriptorToFormattedSignature[actualJvmSignature] =
                        formatSignature(header, expectedJvmSignature?.let { actualJvmSignature }, actualGenericSignature)
            }
        }

        fun check() {
            val formattedActualSignature =
                    if (expectedJvmSignature == null) {
                        Assert.assertTrue(
                                "Expected single declaration, but ${jvmDescriptorToFormattedSignature.keys} found",
                                jvmDescriptorToFormattedSignature.size == 1)

                        jvmDescriptorToFormattedSignature.values.single()
                    }
                    else {
                        jvmDescriptorToFormattedSignature[expectedJvmSignature].sure {
                            "Expected $expectedJvmSignature but only ${jvmDescriptorToFormattedSignature.keys} found for $name"
                        }
                    }

            Assert.assertEquals(expectedFormattedSignature, formattedActualSignature)
        }
    }

    private inner class PackageExpectationsSuite() {
        private val classSuitesByClassName = LinkedHashMap<String, ClassExpectationsSuite>()

        fun getOrCreateClassSuite(className: String): ClassExpectationsSuite =
                classSuitesByClassName.getOrPut(className) { ClassExpectationsSuite(className) }

        fun check() {
            Assert.assertTrue(classSuitesByClassName.isNotEmpty())
            classSuitesByClassName.values.forEach { it.check() }
        }

    }

    private inner class ClassExpectationsSuite(val className: String) {
        val classExpectations = ArrayList<SignatureExpectation>()
        val methodExpectations = ArrayList<SignatureExpectation>()
        val fieldExpectations = ArrayList<SignatureExpectation>()

        fun check() {
            val checker = Checker()
            val classFileName = "$tmpdir/${className.replace('.', '/')}.class"
            val classFile = File(classFileName)

            processClassFile(checker, classFile)

            if (className.endsWith("Package")) {
                // This class is a package facade. We should also check package parts.
                processPackageParts(checker, classFile)
            }

            checkCollectedSignatures()
        }

        private fun processPackageParts(checker: Checker, classFile: File) {
            // Look for package parts in the same directory.
            // Package part file names for package SomePackage look like SomePackage$<hash>.class.
            val classDir = classFile.parentFile
            val classLastName = classFile.name
            val packageFacadePrefix = classLastName.replace(".class", "\$")
            classDir.listFiles { _, lastName ->
                lastName.startsWith(packageFacadePrefix) && lastName.endsWith(".class")
            }.forEach { packageFacadeFile ->
                processClassFile(checker, packageFacadeFile)
            }
        }

        private fun checkCollectedSignatures() {
            (classExpectations + methodExpectations + fieldExpectations).forEach(SignatureExpectation::check)
        }

        private fun processClassFile(checker: Checker, classFile: File) {
            val classInputStream = FileInputStream(classFile)
            try {
                ClassReader(classInputStream).accept(checker,
                                                     ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }
            finally {
                Closeables.closeQuietly(classInputStream)
            }
        }

        private inner class Checker : ClassVisitor(Opcodes.ASM5) {
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                classExpectations.forEach { it.accept(name, name, signature ?: "null") }
                super.visit(version, access, name, signature, superName, interfaces)
            }

            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                methodExpectations.forEach { it.accept(name, desc, signature ?: "null") }
                return super.visitMethod(access, name, desc, signature, exceptions)
            }

            override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                fieldExpectations.forEach { it.accept(name, desc, signature ?: "null") }
                return super.visitField(access, name, desc, signature, value)
            }
        }

        fun addClassExpectation(name: String, jvmSignature: String?, genericSignature: String) {
            classExpectations.add(SignatureExpectation("class: $name", name, jvmSignature, genericSignature))
        }

        fun addFieldExpectation(className: String, memberName: String, jvmSignature: String?, genericSignature: String) {
            fieldExpectations.add(SignatureExpectation("field: $className::$memberName", memberName, jvmSignature, genericSignature))
        }

        fun addMethodExpectation(className: String, memberName: String, jvmSignature: String?, genericSignature: String) {
            methodExpectations.add(SignatureExpectation("method: $className::$memberName", memberName, jvmSignature, genericSignature))
        }
    }

    private fun parseExpectations(ktFile: File): PackageExpectationsSuite {
        val expectations = PackageExpectationsSuite()

        val lines = Files.readLines(ktFile, Charset.forName("utf-8"))
        var lineNo = 0
        while (lineNo < lines.size) {
            val line = lines[lineNo]
            val expectationMatch = expectationRegex.matchExact(line)

            if (expectationMatch != null) {
                val kind = expectationMatch.group(1)!!
                val className = expectationMatch.group(2)!!
                val memberName = expectationMatch.group(4)

                if (kind == "class" && memberName != null) {
                    throw AssertionError("$ktFile:${lineNo+1}: use $className\$$memberName to denote inner class")
                }

                val jvmSignatureMatch = jvmSignatureRegex.matchExact(lines[lineNo+1])
                val genericSignatureMatch = genericSignatureRegex.matchExact(lines[lineNo+1])
                                            ?: genericSignatureRegex.matchExact(lines[lineNo+2])

                if (genericSignatureMatch != null) {
                    val jvmSignature = jvmSignatureMatch?.group(1)
                    val genericSignature = genericSignatureMatch.group(1)

                    val classSuite = expectations.getOrCreateClassSuite(className)

                    when (kind) {
                        "class" -> classSuite.addClassExpectation(className, jvmSignature, genericSignature)
                        "field" -> classSuite.addFieldExpectation(className, memberName, jvmSignature, genericSignature)
                        "method" -> classSuite.addMethodExpectation(className, memberName, jvmSignature, genericSignature)
                        else -> throw AssertionError("$ktFile:${lineNo+1}: unsupported expectation kind: $kind")
                    }

                    // Expectation, skip the following 'jvm signature' and 'generic signature' lines
                    lineNo += 3
                }
                else {
                    throw AssertionError("$ktFile:${lineNo+1}: '$kind' should be followed by 'jvm signature' and 'generic signature'")
                }
            }
            else {
                ++lineNo
            }
        }

        return expectations
    }

    companion object {
        fun formatSignature(header: String, jvmSignature: String?, genericSignature: String): String {
            return listOf(
                    header,
                    jvmSignature?.let { "jvm signature: $it" },
                    "generic signature: $genericSignature"
            ).filterNotNull().joinToString("\n") { "// $it" }
        }

        val expectationRegex = Regex("^// (class|method|field): *([^:]+)(::(.+))? *(//.*)?")
        val jvmSignatureRegex = Regex("^// jvm signature: *(.+) *(//.*)?")
        val genericSignatureRegex = Regex("^// generic signature: *(.+) *(//.*)?")

        fun Regex.matchExact(input: String): MatchResult? {
            val matcher = this.toPattern().matcher(input)
            if (matcher.matches()) {
                return matcher.toMatchResult()
            }
            else {
                return null
            }
        }
    }
}

