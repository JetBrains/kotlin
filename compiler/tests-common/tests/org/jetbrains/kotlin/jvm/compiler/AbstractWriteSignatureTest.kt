/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.*
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.regex.MatchResult

abstract class AbstractWriteSignatureTest : CodegenTestCase() {


    override fun doMultiFileTest(wholeFile: File, files: MutableList<TestFile>, javaFilesDir: File?) {
        compile(files, javaFilesDir)
        try {
            parseExpectations(wholeFile).check()
        } catch (e: Throwable) {
            println(classFileFactory.createText())
            throw e
        }
    }

    private class SignatureExpectation(
        val header: String,
        val name: String,
        val expectedJvmSignature: String?,
        expectedGenericSignature: String
    ) {
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
                        jvmDescriptorToFormattedSignature.size == 1
                    )

                    jvmDescriptorToFormattedSignature.values.single()
                } else {
                    jvmDescriptorToFormattedSignature[expectedJvmSignature].sure {
                        "Expected $expectedJvmSignature but only ${jvmDescriptorToFormattedSignature.keys} found for $name"
                    }
                }

            Assert.assertEquals(expectedFormattedSignature, formattedActualSignature)
        }
    }

    private inner class PackageExpectationsSuite {
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
            val relativeClassFileName = "${className.replace('.', '/')}.class"

            val outputFile = classFileFactory.currentOutput.single { it.relativePath == relativeClassFileName }
            processClassFile(checker, outputFile.asByteArray())

            if (className.endsWith("Package")) {
                // This class is a package facade. We should also check package parts.
                processPackageParts(checker, relativeClassFileName)
            }

            checkCollectedSignatures()
        }

        private fun processPackageParts(checker: Checker, relativeClassFileName: String) {
            // Look for package parts in the same directory.
            // Package part file names for package SomePackage look like SomePackage$<hash>.class.
            val partPrefix = relativeClassFileName.replace(".class", "\$")
            classFileFactory.currentOutput.filter {
                it.relativePath.startsWith(partPrefix) && it.relativePath.endsWith(".class")
            }.forEach { packageFacadeFile ->
                processClassFile(checker, packageFacadeFile.asByteArray())
            }
        }

        private fun checkCollectedSignatures() {
            (classExpectations + methodExpectations + fieldExpectations).forEach(SignatureExpectation::check)
        }

        private fun processClassFile(checker: Checker, classData: ByteArray) {
            ClassReader(classData).accept(
                checker,
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
            )
        }

        private inner class Checker : ClassVisitor(Opcodes.ASM5) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                classExpectations.forEach { it.accept(name, name, signature ?: "null") }
                super.visit(version, access, name, signature, superName, interfaces)
            }

            override fun visitMethod(
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? {
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

        val lines = ktFile.readLines()
        var lineNo = 0
        while (lineNo < lines.size) {
            val line = lines[lineNo]
            val expectationMatch = expectationRegex.matchExact(line)

            if (expectationMatch != null) {
                val kind = expectationMatch.group(1)!!
                val className = expectationMatch.group(2)!!
                val memberName = expectationMatch.group(4)

                if (kind == "class" && memberName != null) {
                    throw AssertionError("$ktFile:${lineNo + 1}: use $className\$$memberName to denote inner class")
                }

                val jvmSignatureMatch = jvmSignatureRegex.matchExact(lines[lineNo + 1])
                val genericSignatureMatch = genericSignatureRegex.matchExact(lines[lineNo + 1])
                    ?: genericSignatureRegex.matchExact(lines[lineNo + 2])

                if (genericSignatureMatch != null) {
                    val jvmSignature = jvmSignatureMatch?.group(1)
                    val genericSignature = genericSignatureMatch.group(1)

                    val classSuite = expectations.getOrCreateClassSuite(className)

                    when (kind) {
                        "class" -> classSuite.addClassExpectation(className, jvmSignature, genericSignature)
                        "field" -> classSuite.addFieldExpectation(className, memberName, jvmSignature, genericSignature)
                        "method" -> classSuite.addMethodExpectation(className, memberName, jvmSignature, genericSignature)
                        else -> throw AssertionError("$ktFile:${lineNo + 1}: unsupported expectation kind: $kind")
                    }

                    // Expectation, skip the following 'jvm signature' and 'generic signature' lines
                    lineNo += 3
                } else {
                    throw AssertionError("$ktFile:${lineNo + 1}: '$kind' should be followed by 'jvm signature' and 'generic signature'")
                }
            } else {
                ++lineNo
            }
        }

        return expectations
    }

    companion object {
        fun formatSignature(header: String, jvmSignature: String?, genericSignature: String): String {
            return listOfNotNull(
                header,
                jvmSignature?.let { "jvm signature: $it" },
                "generic signature: $genericSignature"
            ).joinToString("\n") { "// $it" }
        }

        val expectationRegex = Regex("^// (class|method|field): *([^:]+)(::(.+))? *(//.*)?")
        val jvmSignatureRegex = Regex("^// jvm signature: *(.+) *(//.*)?")
        val genericSignatureRegex = Regex("^// generic signature: *(.+) *(//.*)?")

        fun Regex.matchExact(input: String): MatchResult? {
            val matcher = this.toPattern().matcher(input)
            return if (matcher.matches()) {
                matcher.toMatchResult()
            } else {
                null
            }
        }
    }
}

