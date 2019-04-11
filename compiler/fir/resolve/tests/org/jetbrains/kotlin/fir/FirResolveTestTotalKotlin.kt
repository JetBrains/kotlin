/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.ProjectScope
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Ignore
import java.io.File

@Ignore
@TestDataPath("/")
class FirResolveTestTotalKotlin : AbstractFirResolveWithSessionTestCase() {

    private val forbiddenDirectories = listOf("testdata", "resources")

    override fun createEnvironment(): KotlinCoreEnvironment {

        val configurationKind = ConfigurationKind.ALL
        val testJdkKind = TestJdkKind.FULL_JDK


        val javaFiles = File(".").walkTopDown().onEnter {
            it.name.toLowerCase() !in forbiddenDirectories
        }.filter {
            it.isDirectory
        }.mapNotNull { dir ->
            if (dir.name in setOf("src", "test", "tests")) {
                if (dir.walkTopDown().any { it.extension == "java" }) {
                    return@mapNotNull dir
                }
            }
            null
        }.toList()


        val configuration = KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, emptyList(), javaFiles)
        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    fun testTotalKotlin() {

        val testDataPath = "."
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val allFiles = root.walkTopDown().filter { file ->
            !file.isDirectory && forbiddenDirectories.none { it in file.path.toLowerCase() } && file.extension == "kt"
        }

        val ktFiles = allFiles.map {
            val text = KotlinTestUtils.doLoadFile(it)
            KotlinTestUtils.createFile(it.path, text, project)
        }

        val scope = ProjectScope.getContentScope(project)
        val session = createSession(environment, scope)
        val builder = RawFirBuilder(session, stubMode = false)

        val totalTransformer = FirTotalResolveTransformer()
        val firFiles = ktFiles.toList().progress("Loading FIR").map {
            val firFile = builder.buildFirFile(it)
            (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
            firFile
        }.toList()


        println("Raw FIR up, files: ${firFiles.size}")

        doFirResolveTestBench(firFiles, totalTransformer.transformers, withProgress = true)
    }
}
