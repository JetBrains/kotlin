/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.writeText


internal object TestModuleCompiler {
    fun compileTestModuleToLibrary(module: TestModule, testServices: TestServices): Path {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        for (testFile in module.files) {
            val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
            val tmpSourceFile = (tmpDir / testFile.name).createFile()
            tmpSourceFile.writeText(text)
        }
        return CompilerExecutor.compileLibrary(
            tmpDir,
            CompilerExecutor.parseCompilerOptionsFromTestdata(module),
            compilationErrorExpected = CompilerExecutor.Directives.COMPILATION_ERRORS in module.directives
        )
    }

    fun compileTestModuleToLibrarySources(module: TestModule, testServices: TestServices): Path {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        val librarySourcesPath = tmpDir / "library-sources.jar"
        val manifest = Manifest().apply { mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0" }
        JarOutputStream(librarySourcesPath.outputStream(), manifest).use { jarOutputStream ->
            for (testFile in module.files) {
                val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                addFileToJar(testFile.relativePath, text, jarOutputStream)
            }
        }
        return librarySourcesPath
    }

    private fun addFileToJar(path: String, text: String, jarOutputStream: JarOutputStream) {
        jarOutputStream.putNextEntry(JarEntry(path))
        ByteArrayInputStream(text.toByteArray()).copyTo(jarOutputStream)
        jarOutputStream.closeEntry()
    }
}