/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.asFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.snapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compileAll
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.ProcessBuilder.Redirect

abstract class ClasspathSnapshotTestCommon {

    @get:Rule
    val tmpDir = TemporaryFolder()

    // Use Gson to compare objects
    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    protected fun Any.toGson(): String = gson.toJson(this)

    sealed class SourceFile(val baseDir: File, relativePath: String) {
        val unixStyleRelativePath: String

        init {
            unixStyleRelativePath = relativePath.replace('\\', '/')
        }

        fun asFile() = File(baseDir, unixStyleRelativePath)

        class KotlinSourceFile(baseDir: File, relativePath: String, val preCompiledClassFiles: List<ClassFile>) :
            SourceFile(baseDir, relativePath) {

            constructor(baseDir: File, relativePath: String, preCompiledClassFile: ClassFile) :
                    this(baseDir, relativePath, listOf(preCompiledClassFile))
        }

        class JavaSourceFile(baseDir: File, relativePath: String) : SourceFile(baseDir, relativePath)
    }

    /** Same as [SourceFile] but with a [TemporaryFolder] to store the results of operations on the [SourceFile]. */
    open class TestSourceFile(val sourceFile: SourceFile, private val tmpDir: TemporaryFolder) {

        fun asFile() = sourceFile.asFile()

        /** Compiles this source file and returns all generated .class files. */
        @Suppress("MemberVisibilityCanBePrivate")
        fun compile(): List<ClassFile> = sourceFile.compile(tmpDir)

        fun compileSingle(): ClassFile = compile().single()

        fun compileAndSnapshot() = compileSingle().snapshot()
    }

    object CompileUtil {

        fun SourceFile.compile(tmpDir: TemporaryFolder): List<ClassFile> {
            return if (this is KotlinSourceFile) {
                preCompiledClassFiles.forEach {
                    preCompileKotlinFilesIfNecessary(baseDir, it.classRoot, classpath = emptyList(), tmpDir)
                }
                preCompiledClassFiles
            } else {
                val srcDir = tmpDir.newFolder()
                asFile().copyTo(File(srcDir, unixStyleRelativePath))
                compileAll(srcDir, tmpDir)
            }
        }

        /** Compiles the source files in the given directory and returns all generated .class files. */
        fun compileAll(srcDir: File, tmpDir: TemporaryFolder, classpath: List<File> = emptyList()): List<ClassFile> {
            val kotlinClasses = compileKotlin(srcDir, classpath, tmpDir)

            val javaClasspath = classpath + listOfNotNull(kotlinClasses.firstOrNull()?.classRoot)
            val javaClasses = compileJava(srcDir, javaClasspath, tmpDir)

            return kotlinClasses + javaClasses
        }

        private fun compileKotlin(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            val preCompiledKotlinClassesDir = srcDir.path.let {
                File(it.substringBeforeLast("src") + "classes" + it.substringAfterLast("src"))
            }
            preCompileKotlinFilesIfNecessary(srcDir, preCompiledKotlinClassesDir, classpath, tmpDir)
            return getClassFilesInDir(preCompiledKotlinClassesDir)
        }

        private val preCompiledKotlinClassesDirs = mutableSetOf<File>()

        /**
         * If <kotlin-repo>/dist/kotlinc/lib/kotlin-compiler.jar is available (e.g., by running ./gradlew dist), we will be able to call the
         * Kotlin compiler to generate classes. However, kotlin-compiler.jar is currently not available in CI builds, so we need to
         * pre-compile the classes locally and put them in the test data to check in.
         */
        @Synchronized // To safe-guard shared variable preCompiledKotlinClassesDirs
        private fun preCompileKotlinFilesIfNecessary(
            srcDir: File,
            preCompiledKotlinClassesDir: File,
            classpath: List<File>,
            tmpDir: TemporaryFolder,
            preCompile: Boolean = false // Set to `true` to pre-compile Kotlin class files locally (DO NOT check in with preCompile = true)
        ) {
            if (preCompile) {
                if (!preCompiledKotlinClassesDirs.contains(preCompiledKotlinClassesDir)) {
                    val classFiles = doCompileKotlin(srcDir, classpath, tmpDir)
                    preCompiledKotlinClassesDir.deleteRecursively()
                    for (classFile in classFiles) {
                        File(preCompiledKotlinClassesDir, classFile.unixStyleRelativePath).apply {
                            parentFile.mkdirs()
                            classFile.asFile().copyTo(this)
                        }
                    }
                    preCompiledKotlinClassesDirs.add(preCompiledKotlinClassesDir)
                }
            }
        }

        private fun doCompileKotlin(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            if (srcDir.walk().none { it.path.endsWith(".kt") }) {
                return emptyList()
            }

            val classesDir = tmpDir.newFolder()
            // Note: Calling the following is simpler:
            //     org.jetbrains.kotlin.test.MockLibraryUtil.compileKotlin(
            //         srcDir.path, classesDir, extraClasspath = classpath.map { it.path }.toTypedArray())
            // However, it currently fails with UnsupportedClassVersionError, so we have to launch a new kotlinc process instead
            // (Linux only).
            val commandAndArgs = listOf(
                "dist/kotlinc/bin/kotlinc",
                srcDir.path,
                "-d", classesDir.path,
                "-classpath", (listOf(srcDir) + classpath).joinToString(File.pathSeparator) { it.path }
            )
            runCommandInNewProcess(commandAndArgs)

            return getClassFilesInDir(classesDir)
        }

        private fun compileJava(srcDir: File, classpath: List<File>, tmpDir: TemporaryFolder): List<ClassFile> {
            val javaFiles = srcDir.walk().toList().filter { it.path.endsWith(".java") }
            if (javaFiles.isEmpty()) {
                return emptyList()
            }

            val classesDir = tmpDir.newFolder()
            val classpathOption =
                if (classpath.isNotEmpty()) listOf("-classpath", classpath.joinToString(File.pathSeparator)) else emptyList()

            KotlinTestUtils.compileJavaFiles(javaFiles, listOf("-d", classesDir.path) + classpathOption)
            return getClassFilesInDir(classesDir)
        }

        private fun getClassFilesInDir(classesDir: File): List<ClassFile> {
            return classesDir.walk().toList()
                .filter { it.isFile && it.path.endsWith(".class") }
                .map { ClassFile(classesDir, it.toRelativeString(classesDir)) }
                .sortedBy { it.unixStyleRelativePath.substringBefore(".class") }
        }
    }

    object ClassFileUtil {

        // `ClassFile`s in production code could be in a jar, but the `ClassFile`s in tests are currently in a directory, so converting it
        // to a File is possible.
        fun ClassFile.asFile() = File(classRoot, unixStyleRelativePath)

        fun ClassFile.readBytes() = asFile().readBytes()

        fun ClassFile.snapshot(granularity: ClassSnapshotGranularity? = null): ClassSnapshot = listOf(this).snapshot(granularity).single()

        fun List<ClassFile>.snapshot(granularity: ClassSnapshotGranularity? = null): List<ClassSnapshot> {
            val classes = map { ClassFileWithContents(it, it.readBytes()) }
            return if (granularity == null) {
                ClassSnapshotter.snapshot(classes)
            } else {
                ClassSnapshotter.snapshot(classes, granularity = granularity)
            }
        }
    }
}

internal fun snapshotClasspath(
    classpathSourceDir: File,
    tmpDir: TemporaryFolder,
    granularity: ClassSnapshotGranularity? = null
): ClasspathSnapshot {
    val classpath = mutableListOf<File>()
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.sortedBy { it.name }.map { classpathEntrySourceDir ->
        val classFiles = compileAll(classpathEntrySourceDir, tmpDir, classpath)
        classpath.addAll(listOfNotNull(classFiles.firstOrNull()?.classRoot))

        val relativePaths = classFiles.map { it.unixStyleRelativePath }
        val classSnapshots = classFiles.snapshot(granularity)
        ClasspathEntrySnapshot(
            classSnapshots = relativePaths.zip(classSnapshots).toMap(LinkedHashMap())
        )
    }
    return ClasspathSnapshot(classpathEntrySnapshots)
}

private fun runCommandInNewProcess(commandAndArgs: List<String>) {
    val processBuilder = ProcessBuilder(commandAndArgs)
    processBuilder.redirectInput(Redirect.INHERIT)
    processBuilder.redirectOutput(Redirect.INHERIT)
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()

    val exitCode = try {
        process.waitFor()
    } finally {
        process.destroyForcibly()
    }
    check(exitCode == 0) {
        "Process returned exit code: $exitCode\n" +
                "commandAndArgs = ${commandAndArgs.joinToString(" ")}"
    }
}
