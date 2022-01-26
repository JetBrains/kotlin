/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.benchmarks

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.createSessionForTests
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.File

private fun createFile(shortName: String, text: String, project: Project): KtFile {
    val virtualFile = object : LightVirtualFile(shortName, KotlinLanguage.INSTANCE, text) {
        override fun getPath(): String {
            //TODO: patch LightVirtualFile
            return "/" + name
        }
    }

    virtualFile.charset = CharsetToolkit.UTF8_CHARSET
    val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

    return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
}

private val JDK_PATH = File("${System.getProperty("java.home")!!}/lib/rt.jar")
private val RUNTIME_JAR = File(System.getProperty("kotlin.runtime.path") ?: "dist/kotlinc/lib/kotlin-runtime.jar")

private val LANGUAGE_FEATURE_SETTINGS =
    LanguageVersionSettingsImpl(
        LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3,
        specificFeatures = mapOf(LanguageFeature.NewInference to LanguageFeature.State.ENABLED)
    )

private fun newConfiguration(useNewInference: Boolean): CompilerConfiguration {
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "benchmark")
    configuration.put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, "../compiler/cli/cli-common/resources")
    configuration.addJvmClasspathRoot(JDK_PATH)
    configuration.addJvmClasspathRoot(RUNTIME_JAR)
    configuration.configureJdkClasspathRoots()
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    val newInferenceState = if (useNewInference) LanguageFeature.State.ENABLED else LanguageFeature.State.DISABLED
    configuration.languageVersionSettings = LanguageVersionSettingsImpl(
        LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3,
        specificFeatures = mapOf(
            LanguageFeature.NewInference to newInferenceState
        )
    )
    return configuration
}

@State(Scope.Benchmark)
abstract class AbstractSimpleFileBenchmark {

    private var myDisposable: Disposable = Disposable { }
    private lateinit var env: KotlinCoreEnvironment
    private lateinit var file: KtFile

    @Param("true", "false")
    protected var isIR: Boolean = false

    protected open val useNewInference get() = isIR

    @Setup(Level.Trial)
    fun setUp() {
        if (isIR && !useNewInference) error("Invalid configuration")
        env = KotlinCoreEnvironment.createForTests(
            myDisposable,
            newConfiguration(useNewInference),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        if (isIR) {
            PsiElementFinder.EP.getPoint(env.project).unregisterExtension(JavaElementFinder::class.java)
        }

        file = createFile(
            "test.kt",
            buildText(),
            env.project
        )
    }

    protected fun analyzeGreenFile(bh: Blackhole) {
        if (isIR) {
            analyzeGreenFileIr(bh)
        } else {
            analyzeGreenFileFrontend(bh)
        }
    }

    private fun analyzeGreenFileFrontend(bh: Blackhole) {
        val tracker = ExceptionTracker()
        val storageManager: StorageManager =
            LockBasedStorageManager.createWithExceptionHandling("benchmarks", tracker)

        val context = SimpleGlobalContext(storageManager, tracker)
        val module =
            ModuleDescriptorImpl(
                Name.special("<benchmark>"), storageManager,
                JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
            )
        val moduleContext = context.withProject(env.project).withModule(module)

        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            moduleContext.project,
            listOf(file),
            NoScopeRecordCliBindingTrace(),
            env.configuration,
            { scope -> JvmPackagePartProvider(LANGUAGE_FEATURE_SETTINGS, scope) }
        )

        assert(result.bindingContext.diagnostics.none { it.severity == Severity.ERROR })

        bh.consume(result.shouldGenerateCode)
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    private fun analyzeGreenFileIr(bh: Blackhole) {
        val scope = GlobalSearchScope.filesScope(env.project, listOf(file.virtualFile))
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(env.project))
        val session = createSessionForTests(env.toAbstractProjectEnvironment(), scope.toAbstractProjectFileSearchScope())
        val firProvider = session.firProvider as FirProviderImpl
        val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, PsiHandlingMode.COMPILER)

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
