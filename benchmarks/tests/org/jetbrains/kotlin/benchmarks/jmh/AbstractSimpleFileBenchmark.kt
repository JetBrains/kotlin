/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.benchmarks.jmh

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.FirTestSessionFactoryHelper
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.nio.charset.StandardCharsets

private fun createFile(shortName: String, text: String, project: Project): KtFile {
    val virtualFile = object : LightVirtualFile(shortName, KotlinLanguage.INSTANCE, text) {
        override fun getPath(): String {
            //TODO: patch LightVirtualFile
            return "/" + name
        }
    }

    virtualFile.charset = StandardCharsets.UTF_8
    val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

    return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
}

private val JDK_PATH = File("${System.getProperty("java.home")!!}/lib/rt.jar")
private val RUNTIME_JAR = File(System.getProperty("kotlin.runtime.path") ?: "dist/kotlinc/lib/kotlin-runtime.jar")

private fun newConfiguration(): CompilerConfiguration {
    val configuration = CompilerConfiguration.create()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
    configuration.addJvmClasspathRoot(JDK_PATH)
    configuration.addJvmClasspathRoot(RUNTIME_JAR)
    configuration.configureJdkClasspathRoots()
    @OptIn(MessageCollectorAccess::class) // write access
    configuration.messageCollector = MessageCollector.NONE
    return configuration
}

@State(Scope.Benchmark)
abstract class AbstractSimpleFileBenchmark {

    private var myDisposable: Disposable = Disposable { }
    private lateinit var env: KotlinCoreEnvironment
    private lateinit var file: KtFile

    @Setup(Level.Trial)
    fun setUp() {
        env = KotlinCoreEnvironment.createForTests(
            myDisposable,
            newConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        PsiElementFinder.EP.getPoint(env.project).unregisterExtension(JavaElementFinder::class.java)

        file = createFile(
            "test.kt",
            buildText(),
            env.project
        )
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    protected fun analyzeGreenFile(bh: Blackhole) {
        val scope = GlobalSearchScope.filesScope(env.project, listOf(file.virtualFile))
            .uniteWith(AllJavaSourcesInProjectScope(env.project))
        val session = FirTestSessionFactoryHelper.createSessionForTests(env.toVfsBasedProjectEnvironment(), scope.toAbstractProjectFileSearchScope())
        val firProvider = session.firProvider as FirProviderImpl
        val builder = PsiRawFirBuilder(session, firProvider.kotlinScopeProvider)

        val totalTransformer = FirTotalResolveProcessor(session)
        val firFile = builder.buildFirFile(file).also(firProvider::recordFile)

        totalTransformer.process(listOf(firFile))

        bh.consume(firFile.hashCode())
        env.project.extensionArea
            .getExtensionPoint<PsiElementFinder>(PsiElementFinder.EP.name)
            .unregisterExtension(FirJavaElementFinder::class.java)
    }

    protected abstract fun buildText(): String
}
