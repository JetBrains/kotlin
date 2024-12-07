/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.jvm.compiler

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.KtAssert.assertEquals
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoreJrtFsTest {

    private fun checkClassVersion(expectedVersion: Int, actualClass: VirtualFile?) {
        requireNotNull(actualClass)
        val reader = ClassReader(actualClass.contentsToByteArray())

        val node = ClassNode()
        reader.accept(node, ClassReader.SKIP_CODE)
        assertEquals(
            "Expected class version($expectedVersion) differs in ${actualClass.path} (${node.version})",
            expectedVersion,
            node.version
        )
    }

    lateinit var testRootDisposable: Disposable

    @BeforeEach
    fun createRootDisposable() {
        testRootDisposable = Disposable {}
    }

    @AfterEach
    fun disposeRootDisposable() {
        Disposer.dispose(testRootDisposable)
    }

    /**
     * The test ensures that Thread class always contains version from JDK_11, when such javaHome is used
     * Regardless of compiler runtime JDK
     */
    @Test
    fun testClassVersionsInJavaLangOfJdk11() {
        val configuration = CompilerConfiguration()
        val jdkHome = JvmEnvironmentConfigurator.getJdkHome(TestJdkKind.FULL_JDK_11)
        requireNotNull(jdkHome)
        configuration.put(JVMConfigurationKeys.JDK_HOME, jdkHome)
        val environment =
            KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(this.testRootDisposable, configuration)

        val jrt = environment.jrtFileSystem ?: error("No jrt-fs configured")

        val root = jrt.findFileByPath("$jdkHome!/modules/java.base/java/lang/")
        requireNotNull(root)
        val children = root.children.filter { it.extension == "class" }
        assert(children.isNotEmpty())
        children.forEach { file ->
            checkClassVersion(
                Opcodes.V11,
                file
            )
        }
    }
}