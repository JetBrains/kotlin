/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.analysis.decompiler.stub.file.CachedAttributeData
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readText

abstract class AbstractDecompiledClassTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(
            ApplicationEnvironmentDisposer.ROOT_DISPOSABLE,
            ConfigurationKind.JDK_NO_RUNTIME
        )
    }

    override fun setUp() {
        super.setUp()

        with(environment.projectEnvironment.environment) {
            registerApplicationServices()
        }
    }

    private fun CoreApplicationEnvironment.registerApplicationServices() {
        if (application.getService(FileAttributeService::class.java) == null) {
            registerApplicationService(FileAttributeService::class.java, DummyFileAttributeService)
            registerApplicationService(ClsKotlinBinaryClassCache::class.java, ClsKotlinBinaryClassCache())
        }
    }

    internal fun getClassFileToDecompile(testData: TestData, useStringTable: Boolean): VirtualFile {
        val extraOptions = buildList {
            this.add("-Xallow-kotlin-package")
            if (useStringTable) {
                this.add("-Xuse-type-table")
            }
            this.addAll(testData.additionalCompilerOptions)
        }
        val library = CompilerTestUtil.compileJvmLibrary(
            src = testData.directory.toFile(),
            extraOptions = extraOptions,
        ).toPath()
        return findClassFileByName(
            library, testData.jvmFileName
        )
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

internal data class TestData(
    val directory: Path,
    val mainKotlinFile: Path,
    val expectedFile: Path,
    val jvmFileName: String,
    val additionalCompilerOptions: List<String>,
) {
    companion object {
        fun createFromDirectory(directory: Path): TestData {
            val allFiles = Files.list(directory).collect(Collectors.toList())
            val mainKotlinFile = allFiles.single { path: Path ->
                path.name.replaceFirstChar { Character.toUpperCase(it) } == "${directory.name.removeSuffix("Kt")}.kt"
            }
            val fileText = mainKotlinFile.readText()
            val jvmFileName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "JVM_FILE_NAME:") ?: directory.name
            val additionalCompilerOptions = InTextDirectivesUtils.findListWithPrefixes(fileText, "// !LANGUAGE: ").map { "-XXLanguage:$it" }
            return TestData(
                directory = directory,
                mainKotlinFile = mainKotlinFile,
                expectedFile = allFiles.single { it.fileName.toString() == "${directory.name}.txt" },
                jvmFileName = jvmFileName,
                additionalCompilerOptions = additionalCompilerOptions,
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
