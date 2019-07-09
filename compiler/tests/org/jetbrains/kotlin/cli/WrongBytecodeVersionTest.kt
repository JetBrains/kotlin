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
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.*
import java.io.File

class WrongBytecodeVersionTest : KtUsefulTestCase() {
    private val incompatibleVersion = JvmBytecodeBinaryVersion(42, 0, 0).toArray()

    private fun doTest(relativeDirectory: String, version: IntArray = incompatibleVersion) {
        val directory = KotlinTestUtils.getTestDataPathBase() + relativeDirectory
        val librarySource = File(directory, "A.kt")
        val usageSource = File(directory, "B.kt")

        val tmpdir = KotlinTestUtils.tmpDir(this::class.java.simpleName)

        val environment = KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(testRootDisposable)
        LoadDescriptorUtil.compileKotlinToDirAndGetModule(listOf(librarySource), tmpdir, environment)

        for (classFile in File(tmpdir, "library").listFiles { file -> file.extension == JavaClassFileType.INSTANCE.defaultExtension }) {
            classFile.writeBytes(transformMetadataInClassFile(classFile.readBytes()) { name, _ ->
                if (name == JvmAnnotationNames.BYTECODE_VERSION_FIELD_NAME) version else null
            })
        }

        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(K2JVMCompiler(), listOf(
                usageSource.path,
                "-classpath", tmpdir.path,
                "-d", tmpdir.path
        ))

        assertEquals("Compilation error expected", ExitCode.COMPILATION_ERROR, exitCode)

        val normalized = AbstractCliTest.getNormalizedCompilerOutput(output, exitCode, tmpdir.path)
                .replace("expected version is ${JvmBytecodeBinaryVersion.INSTANCE}", "expected version is \$ABI_VERSION\$")

        KotlinTestUtils.assertEqualsToFile(File(directory, "output.txt"), normalized)
    }

    fun testSimple() {
        doTest("/bytecodeVersion/simple")
    }

    companion object {
        fun transformMetadataInClassFile(bytes: ByteArray, transform: (fieldName: String, value: Any?) -> Any?): ByteArray {
            val writer = ClassWriter(0)
            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION, writer) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
                    val superVisitor = super.visitAnnotation(desc, visible)
                    if (desc == JvmAnnotationNames.METADATA_DESC) {
                        return object : AnnotationVisitor(Opcodes.API_VERSION, superVisitor) {
                            override fun visit(name: String, value: Any) {
                                super.visit(name, transform(name, value) ?: value)
                            }

                            override fun visitArray(name: String): AnnotationVisitor {
                                val entries = arrayListOf<String>()
                                val arrayVisitor = { super.visitArray(name) }
                                return object : AnnotationVisitor(Opcodes.API_VERSION) {
                                    override fun visit(name: String?, value: Any) {
                                        entries.add(value as String)
                                    }

                                    override fun visitEnd() {
                                        @Suppress("UNCHECKED_CAST")
                                        val result = transform(name, entries.toTypedArray()) as Array<String>? ?: entries.toTypedArray()
                                        if (result.isEmpty()) return
                                        with(arrayVisitor()) {
                                            for (value in result) {
                                                visit(null, value)
                                            }
                                            visitEnd()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return superVisitor
                }
            }, 0)
            return writer.toByteArray()
        }
    }
}
