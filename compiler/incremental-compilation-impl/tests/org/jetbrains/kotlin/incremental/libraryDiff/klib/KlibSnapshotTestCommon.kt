/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

abstract class KlibSnapshotTestCommon {

    @field:TempDir
    lateinit var tmpDir: File

    private val sourceFilePaths = mutableMapOf<Long, String>()

    /** E.g. `com/example/Base.kt`, to identify snapshots in test expectations. Works for snapshots of klibs built by [snapshotKlib]. */
    val KlibFileSnapshot.sourceFilePath: String
        get() = sourceFilePaths.getValue(contentHash)

    val klibTestDataDir: File
        get() = ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/libraryDiff/klib")

    fun snapshotKlib(srcDir: File, granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL): KlibEntrySnapshot =
        snapshotKlib(srcDir, tmpDir, granularity)

    /** Compiles the Kotlin sources in [srcDir] to a klib and snapshots it. */
    private fun snapshotKlib(
        srcDir: File,
        tmpDir: File,
        granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL,
    ): KlibEntrySnapshot {
        val klib = compileToKlib(srcDir, Files.createTempDirectory(tmpDir.toPath(), "klib").toFile())
        // The content hash does not depend on the granularity, so the class-member-level snapshot gives the paths for both
        KlibEntrySnapshotter.snapshot(klib, settings(CLASS_MEMBER_LEVEL)).klibFileSnapshots.forEach {
            sourceFilePaths[it.contentHash] = it.computeSourceFilePath()
        }
        return KlibEntrySnapshotter.snapshot(klib, settings(granularity))
    }

    private fun settings(granularity: ClassSnapshotGranularity) =
        ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses = false, expandTypeAliases = false)

    // TODO(KT-91548): read it from the fragment itself once it is stored at the package fragment level.
    private fun KlibFileSnapshot.computeSourceFilePath(): String {
        val fragment = parsePackageFragment(classMemberLevelSnapshot!!.packageFragmentProto)
        val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)
        val fileId = fragment.class_List.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.classFile) }
            ?: fragment.`package`.functionList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.functionFile) }
            ?: fragment.`package`.propertyList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile) }
            ?: fragment.`package`.typeAliasList.firstNotNullOfOrNull { it.getExtensionOrNull(KlibMetadataProtoBuf.typeAliasFile) }
        val sourceFile = fileId?.let { nameResolver.getString(it) } ?: "<package>"
        return if (packageName.isRoot) sourceFile else "${packageName.asString().replace('.', '/')}/$sourceFile"
    }

    /** Compiles the Kotlin sources in [srcDir] to a klib file in [outputDir] and returns it. */
    private fun compileToKlib(srcDir: File, outputDir: File, moduleName: String = "test"): File {
        val sources = srcDir.walk().filter { it.isFile && it.extension == "kt" }.map { it.path }.sorted().toList()
        check(sources.isNotEmpty()) { "No Kotlin sources in $srcDir" }

        val arguments = K2JSCompilerArguments().apply {
            freeArgs = sources
            libraries = ForTestCompileRuntime.stdlibJs().absolutePath
            this.outputDir = outputDir.path
            this.moduleName = moduleName
        }
        val messageCollector = MessageCollectorImpl()
        val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, arguments)
        check(exitCode == ExitCode.OK) { "Failed to compile $srcDir to a klib:\n$messageCollector" }

        return File(outputDir, "$moduleName.klib").also { check(it.isFile) { "Klib not found: $it" } }
    }
}
