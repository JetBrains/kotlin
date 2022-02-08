/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubElement
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.CachedAttributeData
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.impl.STUB_TO_STRING_PREFIX
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.toList

abstract class AbstractClsStubBuilderTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(testRootDisposable, ConfigurationKind.JDK_NO_RUNTIME)
    }

    override fun setUp() {
        super.setUp()

        with(environment.projectEnvironment.environment) {
            registerApplicationServices()
        }
    }

    private fun CoreApplicationEnvironment.registerApplicationServices() {
        registerApplicationService(FileAttributeService::class.java, DummyFileAttributeService)
        registerApplicationService(ClsKotlinBinaryClassCache::class.java, ClsKotlinBinaryClassCache())
    }

    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        val testData = TestData.createFromDirectory(testDirectoryPath)

        doTest(testData, useStringTable = true)
        doTest(testData, useStringTable = false)
    }


    private fun doTest(testData: TestData, useStringTable: Boolean) {
        val classFile = getClassFileToDecompile(testData, useStringTable)
        testClsStubsForFile(classFile, testData)
    }

    private fun testClsStubsForFile(classFile: VirtualFile, testData: TestData) {
        val stubTreeFromCls = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(classFile))!!
        KotlinTestUtils.assertEqualsToFile(testData.expectedFile, stubTreeFromCls.serializeToString())
    }

    private fun getClassFileToDecompile(testData: TestData, useStringTable: Boolean): VirtualFile {
        val extraOptions = buildList {
            add("-Xallow-kotlin-package")
            if (useStringTable) {
                add("-Xuse-type-table")
            }
        }

        val library = CompilerTestUtil.compileJvmLibrary(
            src = testData.directory.toFile(),
            extraOptions = extraOptions,
        ).toPath()

        return findClassFileByName(library, testData.jvmFileName)
    }

    private fun findClassFileByName(library: Path, className: String): VirtualFile {
        val jarFileSystem = environment.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
        val root = jarFileSystem.refreshAndFindFileByPath(library.absolutePathString() + "!/")!!
        val files = mutableSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(
            root,
            /*filter=*/{ virtualFile ->
                virtualFile.isDirectory || virtualFile.name == "$className.class"
            },
            /*iterator=*/{ virtualFile ->
                if (!virtualFile.isDirectory) {
                    files.addIfNotNull(virtualFile)
                }
                true
            })

        return files.single()
    }

}

private fun StubElement<out PsiElement>.serializeToString(): String {
    return serializeStubToString(this)
}

private fun serializeStubToString(stubElement: StubElement<*>): String {
    val treeStr = DebugUtil.stubTreeToString(stubElement).replace(SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME.asString(), "<no name>")

    // Nodes are stored in form "NodeType:Node" and have too many repeating information for Kotlin stubs
    // Remove all repeating information (See KotlinStubBaseImpl.toString())
    return treeStr.lines().joinToString(separator = "\n") {
        if (it.contains(STUB_TO_STRING_PREFIX)) {
            it.takeWhile(Char::isWhitespace) + it.substringAfter("KotlinStub$")
        } else {
            it
        }
    }.replace(", [", "[")
}


private data class TestData(
    val directory: Path,
    val mainKotlinFile: Path,
    val expectedFile: Path,
    val jvmFileName: String
) {
    companion object {
        fun createFromDirectory(directory: Path): TestData {
            val allFiles = Files.list(directory).toList()
            val mainKotlinFile = allFiles.single { path ->
                path.name.replaceFirstChar { it.uppercaseChar() } == "${directory.name.removeSuffix("Kt")}.kt"
            }
            val jvmFileName = InTextDirectivesUtils.findStringWithPrefixes(mainKotlinFile.readText(), "JVM_FILE_NAME:")
                ?: directory.name
            return TestData(
                directory = directory,
                mainKotlinFile = mainKotlinFile,
                expectedFile = allFiles.single { it.name == "${directory.name}.txt" },
                jvmFileName = jvmFileName,
            )
        }
    }
}

object DummyFileAttributeService : FileAttributeService {
    override fun <T> write(file: VirtualFile, id: String, value: T, writeValueFun: (DataOutput, T) -> Unit): CachedAttributeData<T> {
        return CachedAttributeData(value, 0)
    }

    override fun <T> read(file: VirtualFile, id: String, readValueFun: (DataInput) -> T): CachedAttributeData<T>? {
        return null
    }
}
