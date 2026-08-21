/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import javax.tools.ToolProvider

/**
 * A `.class` file of the previous build is read by the session which may see the output directory, while the
 * references recorded in it are resolved on the whole classpath.
 *
 * An incremental round is the only compilation which gives a session a proper part of the classpath: the output
 * directory of the previous build is prepended to the classpath and read a second time as a classpath of its own
 * (see [IncrementalJvmCompilerRunnerBase.performWorkBeforeCompilation] and
 * `prepareIncrementalCompilationContextAndLibrariesClasspath`). Only the precompiled-binaries session may see
 * that directory, and the libraries session is the rest of the classpath, with the directory excluded.
 *
 * The Java classes below have the same shape as those a build system compiling Java into the same output
 * directory leaves behind (Maven does, which is how KT-17897 was found): `p.Ref` lies in the output directory,
 * so only the precompiled-binaries session reads it, and the `p.Lib.Nested` its signature names lies in a
 * library that session may not see.
 */
class IncrementalJavaClassFromPreviousOutputTest : AbstractIncrementalK2JvmCompilerRunnerTest() {

    @Test
    fun testTheReferenceOfAPreviousOutputClassIsResolvedInTheLibraries() {
        val sourceRoot = File(workingDir, "src").apply { mkdirs() }
        val outputDirectory = File(workingDir, "out").apply { mkdirs() }
        val cacheDirectory = File(workingDir, "caches").apply { mkdirs() }

        val library = compileJava(
            "library",
            "p/Lib.java" to "package p; public class Lib { public static class Nested { public String name() { return \"nested\"; } } }",
        )
        val previousJavaOutput = compileJava(
            "previous-java-output",
            "p/Ref.java" to "package p; public class Ref { public Lib.Nested get() { return null; } }",
            classpath = library,
        )
        forgetWhichClassesAreNested(File(previousJavaOutput, "p/Ref.class"))

        val usage = File(sourceRoot, "usage.kt")
        usage.writeText("package p\n\nfun unrelated(): Int = 1\n")

        val arguments = createCompilerArguments(outputDirectory, workingDir).apply {
            classpath = classpath + File.pathSeparator + library.path
        }

        val firstBuild = make(cacheDirectory, outputDirectory, listOf(sourceRoot), arguments)
        assertEquals(ExitCode.OK, firstBuild.exitCode, firstBuild.compileErrors.joinToString("\n"))

        // What the build being incremental now means for `p.Ref`: it is on the classpath of the round, and only
        // the session which reads the output of the previous build may see it.
        previousJavaOutput.copyRecursively(outputDirectory, overwrite = true)
        usage.writeText("package p\n\nfun unrelated(): Int = 1\n\nfun useRef(): String = Ref().get().name()\n")

        val incrementalBuild = make(cacheDirectory, outputDirectory, listOf(sourceRoot), arguments)
        assertEquals(
            ExitCode.OK, incrementalBuild.exitCode,
            "the return type of Ref.get() has to be the p.Lib.Nested of the library, which the session reading " +
                    "Ref may not see: " + incrementalBuild.compileErrors.joinToString("\n")
        )
    }

    /**
     * Removes the `InnerClasses` attribute of [classFile], which is what makes the reader of the class file
     * resolve the references recorded in it at all.
     *
     * The attribute states which of the names in the class file are nested classes, and `javac` writes it for
     * every class it mentions, so the reader can tell `p.Lib$Nested` (nested) from a top-level class of that
     * name without loading anything. Compilers other than `javac` do not always write it (see the Groovy case in
     * [org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext.resolveByInternalName]),
     * and then the reader has to load the referenced class to find out — the lookup that must reach the whole
     * classpath, not only the part the reader may see.
     */
    private fun forgetWhichClassesAreNested(classFile: File) {
        val writer = ClassWriter(0)
        ClassReader(classFile.readBytes()).accept(
            object : ClassVisitor(Opcodes.API_VERSION, writer) {
                override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {}
            },
            0,
        )
        classFile.writeBytes(writer.toByteArray())
    }

    /** Compiles [sources], given as pairs of a path relative to the source root and its text, into `$name`. */
    private fun compileJava(name: String, vararg sources: Pair<String, String>, classpath: File? = null): File {
        val sourceFiles = sources.map { source ->
            File(workingDir, "$name-src/${source.first}").apply {
                parentFile.mkdirs()
                writeText(source.second)
            }
        }
        val destination = File(workingDir, name).apply { mkdirs() }
        val options = buildList {
            if (classpath != null) {
                add("-cp"); add(classpath.path)
            }
            add("-d"); add(destination.path)
            addAll(sourceFiles.map { it.path })
        }.toTypedArray()

        val errors = ByteArrayOutputStream()
        val exitCode = ToolProvider.getSystemJavaCompiler().run(null, null, errors, *options)
        check(exitCode == 0) { "javac failed: $errors" }
        return destination
    }
}
