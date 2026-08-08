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
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
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
import org.jetbrains.kotlin.fir.declarations.FirFile
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

// Legacy JDK 8 bootstrap jar; absent on JDK 9+ where configureJdkClasspathRoots() is enough.
private val JDK_PATH = File("${System.getProperty("java.home")!!}/lib/rt.jar")

/**
 * Resolves kotlin-stdlib for the FIR session classpath.
 *
 * Order: `-Dkotlin.runtime.path`, then common relative locations from repo root or `:benchmarks`
 * working directory. The historical default `kotlin-runtime.jar` is no longer shipped in dist.
 */
private fun resolveRuntimeJar(): File {
    System.getProperty("kotlin.runtime.path")?.let { explicit ->
        return File(explicit)
    }
    val relativeCandidates = listOf(
        "dist/kotlinc/lib/kotlin-stdlib.jar",
        "../dist/kotlinc/lib/kotlin-stdlib.jar",
    )
    for (relative in relativeCandidates) {
        val candidate = File(relative)
        if (candidate.isFile) return candidate
    }
    // Last resort: walk up from user.dir looking for dist/kotlinc/lib/kotlin-stdlib.jar
    var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
    repeat(6) {
        val candidate = File(dir, "dist/kotlinc/lib/kotlin-stdlib.jar")
        if (candidate.isFile) return candidate
        dir = dir?.parentFile
    }
    error(
        "Kotlin stdlib not found. Build the dist or pass " +
                "-Dkotlin.runtime.path=/path/to/kotlin-stdlib.jar (searched from ${System.getProperty("user.dir")})"
    )
}

private fun newConfiguration(): CompilerConfiguration {
    val configuration = CompilerConfiguration.create()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
    if (JDK_PATH.isFile) {
        configuration.addJvmClasspathRoot(JDK_PATH)
    }
    val runtimeJar = resolveRuntimeJar()
    configuration.addJvmClasspathRoot(runtimeJar)
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

    // Per-invocation FIR state for [prepareFirForResolve] / [processFirResolve].
    private lateinit var resolveProcessor: FirTotalResolveProcessor
    private lateinit var firFile: FirFile

    @Setup(Level.Trial)
    fun setUp() {
        @OptIn(CoreEnvironmentDeprecation::class)
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

    /**
     * Legacy entry used by existing benchmarks: builds raw FIR and runs resolve inside the
     * timed method. Prefer [prepareFirForResolve] + [processFirResolve] for new benchmarks
     * when raw FIR should stay outside the measured path.
     */
    @OptIn(ObsoleteTestInfrastructure::class)
    protected fun analyzeGreenFile(bh: Blackhole) {
        prepareFirForResolve()
        processFirResolve(bh)
    }

    /**
     * Creates a fresh FIR session and raw FIR file for the next resolve.
     *
     * Call from `@Setup(Level.Invocation)` so session/raw-FIR work is not attributed to the
     * timed benchmark method. FIR is mutated in place by resolve, so this must run every
     * invocation. Does not run checkers or codegen.
     */
    @OptIn(ObsoleteTestInfrastructure::class)
    protected fun prepareFirForResolve() {
        val scope = GlobalSearchScope.filesScope(env.project, listOf(file.virtualFile))
            .uniteWith(AllJavaSourcesInProjectScope(env.project))
        val session = FirTestSessionFactoryHelper.createSessionForTests(
            env.toVfsBasedProjectEnvironment(),
            scope.toAbstractProjectFileSearchScope()
        )
        val firProvider = session.firProvider as FirProviderImpl
        val builder = PsiRawFirBuilder(session, firProvider.kotlinScopeProvider)
        resolveProcessor = FirTotalResolveProcessor(session)
        firFile = builder.buildFirFile(file).also(firProvider::recordFile)
    }

    /**
     * Runs [FirTotalResolveProcessor.process] only (same stack as `FirSession.runResolution`).
     * Requires a prior [prepareFirForResolve] on the same invocation. Checkers/codegen are not run.
     */
    protected fun processFirResolve(bh: Blackhole) {
        resolveProcessor.process(listOf(firFile))

        bh.consume(firFile.hashCode())
        env.project.extensionArea
            .getExtensionPoint<PsiElementFinder>(PsiElementFinder.EP.name)
            .unregisterExtension(FirJavaElementFinder::class.java)
    }

    protected abstract fun buildText(): String
}
