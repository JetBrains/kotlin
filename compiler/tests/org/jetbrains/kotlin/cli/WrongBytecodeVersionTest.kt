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

package org.jetbrains.kotlin.cli

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.*
import java.io.File

class WrongBytecodeVersionTest : UsefulTestCase() {
    private val incompatibleVersion = JvmBytecodeBinaryVersion(42, 0, 0).toArray()

    private fun doTest(relativeDirectory: String) {
        val directory = KotlinTestUtils.getTestDataPathBase() + relativeDirectory
        val librarySource = File(directory, "A.kt")
        val usageSource = File(directory, "B.kt")

        val tmpdir = KotlinTestUtils.tmpDir(javaClass.simpleName)
        LoadDescriptorUtil.compileKotlinToDirAndGetAnalysisResult(
                listOf(librarySource), tmpdir, testRootDisposable, ConfigurationKind.ALL, false
        )

        for (classFile in File(tmpdir, "library").listFiles { file -> file.extension == JavaClassFileType.INSTANCE.defaultExtension }) {
            changeVersionInBytecode(classFile)
        }

        val (output, exitCode) = CliBaseTest.executeCompilerGrabOutput(K2JVMCompiler(), listOf(
                usageSource.path,
                "-classpath", tmpdir.path,
                "-d", tmpdir.path
        ))

        assertEquals("Compilation error expected", ExitCode.COMPILATION_ERROR, exitCode)

        val normalized = CliBaseTest.getNormalizedCompilerOutput(output, exitCode, tmpdir.path, JvmBytecodeBinaryVersion.INSTANCE)

        KotlinTestUtils.assertEqualsToFile(File(directory, "output.txt"), normalized)
    }

    private fun changeVersionInBytecode(file: File) {
        val writer = ClassWriter(0)
        ClassReader(file.inputStream()).accept(object : ClassVisitor(Opcodes.ASM5, writer) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                val superVisitor = super.visitAnnotation(desc, visible)!!
                if (desc == JvmAnnotationNames.METADATA_DESC) {
                    return object : AnnotationVisitor(Opcodes.ASM5, superVisitor) {
                        override fun visit(name: String?, value: Any) {
                            val updatedValue: Any =
                                    if (name == JvmAnnotationNames.BYTECODE_VERSION_FIELD_NAME) incompatibleVersion
                                    else value
                            super.visit(name, updatedValue)
                        }
                    }
                }
                return superVisitor
            }
        }, 0)
        file.writeBytes(writer.toByteArray())
    }

    fun testSimple() {
        doTest("/bytecodeVersion/simple")
    }
}
