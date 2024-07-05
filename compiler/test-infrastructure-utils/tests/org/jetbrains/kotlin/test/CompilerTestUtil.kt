/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.Usage
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.File.Companion.userDir
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.PathUtil.kotlinPathsForDistDirectory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

object CompilerTestUtil {
    @JvmStatic
    fun executeCompilerAssertSuccessful(compiler: CLITool<*>, args: List<String>, messageRenderer: MessageRenderer? = null) {
        val (output, exitCode) = executeCompiler(compiler, args, messageRenderer)
        KtAssert.assertEquals(output, ExitCode.OK, exitCode)
    }

    @JvmStatic
    fun executeCompiler(compiler: CLITool<*>, args: List<String>, messageRenderer: MessageRenderer? = null): Pair<String, ExitCode> {
        val bytes = ByteArrayOutputStream()
        val origErr = System.err
        try {
            System.setErr(PrintStream(bytes))
            val exitCode =
                if (messageRenderer == null) CLITool.doMainNoExit(compiler, args.toTypedArray())
                else CLITool.doMainNoExit(compiler, args.toTypedArray(), messageRenderer)
            return Pair(String(bytes.toByteArray()), exitCode)
        } finally {
            System.setErr(origErr)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun compileJvmLibrary(
            src: File,
            libraryName: String = "library",
            extraOptions: List<String> = emptyList(),
            extraClasspath: List<File> = emptyList(),
            messageRenderer: MessageRenderer? = null,
    ): File {
        val destination = File(KtTestUtil.tmpDir("testLibrary"), "$libraryName.jar")
        val args = mutableListOf<String>().apply {
            add(src.path)
            add("-d")
            add(destination.path)
            if (extraClasspath.isNotEmpty()) {
                add("-cp")
                add(extraClasspath.joinToString(":") { it.path })
            }
            addAll(extraOptions)
        }
        executeCompilerAssertSuccessful(K2JVMCompiler(), args, messageRenderer)
        return destination
    }

    @JvmStatic
    fun normalizeCompilerOutput(output: String, tmpdir: String): String {
        val tmpDirAbsoluteDir = File(tmpdir).absolutePath
        return StringUtil.convertLineSeparators(output)
            .replace(kotlinPathsForDistDirectory.homePath.absolutePath, "\$PROJECT_DIR$")
            .replace(kotlinPathsForDistDirectory.homePath.parentFile.absolutePath, "\$DIST_DIR$")
            .replace(userDir.absolutePath, "\$USER_DIR$")
            .replace(tmpDirAbsoluteDir, "\$TMP_DIR$")
            .replace("\\", "/")
            .replace(KtTestUtil.getJdk8Home().absolutePath.replace("\\", "/"), "\$JDK_1_8")
            .replace(KtTestUtil.getJdk11Home().absolutePath.replace("\\", "/"), "\$JDK_11")
            .replace(KtTestUtil.getJdk17Home().absolutePath.replace("\\", "/"), "\$JDK_17")
            .replace(KtTestUtil.getJdk21Home().absolutePath.replace("\\", "/"), "\$JDK_21")
            .replace("info: executable production duration: \\d+ms".toRegex(), "info: executable production duration: [time]")
            .replace(KotlinCompilerVersion.VERSION, "\$VERSION$")
            .replace(System.getProperty("java.runtime.version"), "\$JVM_VERSION$")
            .replace(" " + JvmMetadataVersion.INSTANCE, " \$ABI_VERSION$")
            .replace(" " + JsMetadataVersion.INSTANCE, " \$ABI_VERSION$")
            .replace(" " + JvmMetadataVersion.INSTANCE_NEXT, " \$ABI_VERSION_NEXT$")
            .replace("\n" + Usage.BAT_DELIMITER_CHARACTERS_NOTE + "\n", "")
            .replace("log4j:WARN.*\n".toRegex(), "")
    }
}
