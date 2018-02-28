/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.createTask
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.tools.kompot.commons.classpathToClassFiles
import org.jetbrains.kotlin.tools.kompot.idea.merger.IdeMergedVersionHandler
import org.jetbrains.kotlin.tools.kompot.idea.merger.IdeMergedVersionLoader
import org.jetbrains.kotlin.tools.kompot.idea.merger.ReplaceNullCompareHandler
import org.jetbrains.kotlin.tools.kompot.verifier.ClassReadVersionInfoProvider
import org.jetbrains.kotlin.tools.kompot.verifier.Verifier
import org.objectweb.asm.ClassReader
import java.io.File
import java.io.InputStream
import java.nio.file.Files

fun Project.configureVerification() {
    createTask("verifyBytecode", Task::class) {

        val compileOnly by configurations

        val classes = tasks.getByName("classes")
        dependsOn(classes)


        doFirst {

            val icDep = compileOnly.files {
                it is FileCollectionDependency
            }

            val verifyFiles = the<JavaPluginConvention>().sourceSets.flatMap { it.output.classesDirs }

            val loader = IdeMergedVersionLoader()
            val infoProvider = ClassReadVersionInfoProvider(loader)
            val handler = IdeMergedVersionHandler()
            val correctedHandler = ReplaceNullCompareHandler(loader.load("IC-173.4548.28, IC-181.3741.2"), handler)

            fun processClasses(dep: Iterable<File>, process: (InputStream) -> Unit) {
                classpathToClassFiles(dep).map {
                    Files.newInputStream(it)
                }.forEach {
                    it.use {
                        process(it)
                    }
                }
            }

            var indexSize = 0

            fun loadToIndex(inputStream: InputStream) {
                ClassReader(inputStream).accept(infoProvider.createVisitor(), ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
                indexSize++
            }

            var failed = false
            var verified = 0

            fun verify(inputStream: InputStream) {
                val verifier = Verifier(infoProvider, loader, correctedHandler)
                ClassReader(inputStream).accept(verifier.visitor, ClassReader.SKIP_FRAMES)
                verified++
                verifier.problems.forEach { logger.error(it.toString()) }
                if (verifier.problems.isNotEmpty()) {
                    failed = true
                }
            }

            processClasses(icDep) {
                loadToIndex(it)
            }
            processClasses(verifyFiles) {
                loadToIndex(it)
            }

            processClasses(verifyFiles) {
                verify(it)
            }

            if (failed) {
                throw GradleException("Verification failed")
            } else {
                logger.info("""
                    Index sizes
                    C: ${infoProvider.classToVersionInfo.size}
                    M: ${infoProvider.methodToVersionInfo.size}
                    F: ${infoProvider.fieldToVersionInfo.size}
                    """.trimIndent())
                logger.info("Verification index size: $indexSize\nVerified: $verified")
            }

        }
    }
}