/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.javaInterop

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.CompilationMode
import org.jetbrains.kotlin.incremental.dirtyFiles.DirtyFilesContainer
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File

private class CoarseJavaInteropCoordinator(
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    messageCollector: MessageCollector,
) : JavaInteropCoordinator(messageCollector) {
    private val javaFilesProcessor =
        ChangedJavaFilesProcessor(reporter) { getPsiJavaFile(it) }

    override fun analyzeChangesInJavaSources(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        mutableDirtyFiles: DirtyFilesContainer
    ): CompilationMode.Rebuild? {
        val javaFilesChanges = javaFilesProcessor.process(changedFiles)
        val affectedJavaSymbols = when (javaFilesChanges) {
            is ChangesEither.Known -> javaFilesChanges.lookupSymbols
            is ChangesEither.Unknown -> return CompilationMode.Rebuild(javaFilesChanges.reason)
        }
        mutableDirtyFiles.addByDirtySymbols(affectedJavaSymbols)
        return null
    }

    override fun getAdditionalDirtyLookupSymbols(): Iterable<LookupSymbol> {
        return javaFilesProcessor.allChangedSymbols
    }
}

private class PreciseJavaInteropCoordinator(
    private val reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    messageCollector: MessageCollector,
) : JavaInteropCoordinator(messageCollector) {
    private val changedUntrackedJavaClasses = mutableSetOf<ClassId>()

    override fun hasChangedUntrackedJavaClasses(): Boolean = changedUntrackedJavaClasses.isNotEmpty()

    override fun makeJavaClassesTracker(platformCache: IncrementalJvmCache): JavaClassesTracker? {
        val changesTracker = JavaClassesTrackerImpl(
            platformCache, changedUntrackedJavaClasses.toSet(),
            compilerConfiguration.languageVersionSettings,
        )
        changedUntrackedJavaClasses.clear()
        return changesTracker
    }

    override fun analyzeChangesInJavaSources(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        mutableDirtyFiles: DirtyFilesContainer
    ): CompilationMode.Rebuild? {
        val rebuildReason = processChangedJava(changedFiles, caches)
        if (rebuildReason != null) return CompilationMode.Rebuild(rebuildReason)
        return null
    }

    private fun processChangedJava(changedFiles: DeterminableFiles.Known, caches: IncrementalJvmCachesManager): BuildAttribute? {
        val javaFiles = (changedFiles.modified + changedFiles.removed).filter(File::isJavaFile)

        for (javaFile in javaFiles) {
            if (!caches.platformCache.isTrackedFile(javaFile)) {
                if (!javaFile.exists()) {
                    // todo: can we do this more optimal?
                    reporter.info { "Could not get changed for untracked removed java file $javaFile" }
                    return BuildAttribute.JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED
                }

                val psiFile = getPsiJavaFile(javaFile)
                if (psiFile !is PsiJavaFile) {
                    reporter.info { "[Precise Java tracking] Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                    return BuildAttribute.JAVA_CHANGE_UNEXPECTED_PSI
                }

                for (psiClass in psiFile.classes) {
                    val qualifiedName = psiClass.qualifiedName
                    if (qualifiedName == null) {
                        reporter.info { "[Precise Java tracking] Class with unknown qualified name in $javaFile" }
                        return BuildAttribute.JAVA_CHANGE_UNKNOWN_QUALIFIER
                    }

                    processChangedUntrackedJavaClass(psiClass, ClassId.topLevel(FqName(qualifiedName)))
                }
            }
        }

        caches.platformCache.markDirty(javaFiles)
        return null
    }

    private fun processChangedUntrackedJavaClass(psiClass: PsiClass, classId: ClassId) {
        changedUntrackedJavaClasses.add(classId)
        for (innerClass in psiClass.innerClasses) {
            val name = innerClass.name ?: continue
            processChangedUntrackedJavaClass(innerClass, classId.createNestedClassId(Name.identifier(name)))
        }
    }
}

/**
 * Common logic of IncrementalCompilerRunner related to kotlin+java interop
 */
internal sealed class JavaInteropCoordinator(
    messageCollector: MessageCollector,
) {
    protected val compilerConfiguration: CompilerConfiguration by lazy {
        val filterMessageCollector = FilteringMessageCollector(messageCollector) { !it.isError }
        CompilerConfiguration().apply {
            this.messageCollector = filterMessageCollector
            configureJdkClasspathRoots()
        }
    }

    private val psiFileFactory: PsiFileFactory by lazy {
        val rootDisposable =
            Disposer.newDisposable("Disposable for PSI file factory of ${IncrementalJvmCompilerRunner::class.simpleName}")
        val configuration = compilerConfiguration
        val environment =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        PsiFileFactory.getInstance(project)
    }

    protected fun getPsiJavaFile(file: File): PsiFile? =
        psiFileFactory.createFileFromText(file.nameWithoutExtension, JavaLanguage.INSTANCE, file.readText())

    /**
     * Unfortunately different JavaInterop implementations interface with ICRunner differently,
     * so some APIs are valid for PreciseJavaTracking and others are valid for the non-precise one.
     * Still, it's useful to have a defined api boundary, even if it's more like two boundaries.
     */

    open fun hasChangedUntrackedJavaClasses(): Boolean = false

    open fun getAdditionalDirtyLookupSymbols(): Iterable<LookupSymbol> = emptyList()

    open fun makeJavaClassesTracker(platformCache: IncrementalJvmCache): JavaClassesTracker? = null

    abstract fun analyzeChangesInJavaSources(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        mutableDirtyFiles: DirtyFilesContainer
    ): CompilationMode.Rebuild?

    companion object {
        fun getImplementation(
            usePreciseJavaTracking: Boolean,
            reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
            messageCollector: MessageCollector,
        ): JavaInteropCoordinator {
            return if (usePreciseJavaTracking) {
                PreciseJavaInteropCoordinator(reporter, messageCollector)
            } else {
                CoarseJavaInteropCoordinator(reporter, messageCollector)
            }
        }
    }
}
