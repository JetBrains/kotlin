/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import com.intellij.mock.MockVirtualFileSystem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.KtSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.calls.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.streams.asSequence
import org.junit.jupiter.api.Assertions
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun testDataPath(path: String): Path {
    return Paths.get("analysis/analysis-api-standalone/testData/sessionBuilder").resolve(path)
}

fun KtCallExpression.assertIsCallOf(callableId: CallableId) {
    analyze(this) {
        val ktCallInfo = resolveCall()
        Assertions.assertInstanceOf(KtSuccessCallInfo::class.java, ktCallInfo); ktCallInfo as KtSuccessCallInfo
        val symbol = ktCallInfo.successfulFunctionCallOrNull()?.symbol
        Assertions.assertInstanceOf(KtFunctionSymbol::class.java, symbol); symbol as KtFunctionSymbol
        Assertions.assertEquals(callableId, symbol.callableIdIfNonLocal)
    }
}

internal fun compileCommonKlib(kLibSourcesRoot: Path): Path {
    val ktFiles = Files.walk(kLibSourcesRoot).asSequence().filter { it.extension == "kt" }.toList()
    val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib").toPath()

    val arguments = buildList {
        ktFiles.mapTo(this) { it.absolutePathString() }
        add("-d")
        add(testKlib.absolutePathString())
    }
    MockLibraryUtil.runMetadataCompiler(arguments)

    return testKlib
}

internal fun createVirtualFileOnTheFly(
    project: Project,
    text: String,
    fs: VirtualFileSystem = MockVirtualFileSystem(),
): VirtualFile {
    val factory = KtPsiFactory(project, markGenerated = false)
    val ktFile = factory.createFile(text)
    return object : VirtualFile() {
        override fun getName(): String = ktFile.name

        override fun getFileSystem(): VirtualFileSystem = fs

        override fun getPath(): String {
            error("Not yet implemented")
        }

        override fun isWritable(): Boolean = false

        override fun isDirectory(): Boolean = false

        override fun isValid(): Boolean = ktFile.isValid

        override fun getParent(): VirtualFile {
            error("Not yet implemented")
        }

        override fun getChildren(): Array<VirtualFile> = emptyArray()

        override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
            error("Not yet implemented")
        }

        override fun contentsToByteArray(): ByteArray = ktFile.text.toByteArray()

        private val timeStamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        override fun getTimeStamp(): Long = timeStamp

        override fun getLength(): Long = ktFile.textLength.toLong()

        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
            error("Not yet implemented")
        }

        override fun getInputStream(): InputStream {
            error("Not yet implemented")
        }
    }
}
