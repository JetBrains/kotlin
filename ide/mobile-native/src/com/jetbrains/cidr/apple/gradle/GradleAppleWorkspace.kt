package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKindProvider
import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCLanguageKindCalculator
import com.jetbrains.cidr.lang.workspace.OCVariant
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File

class GradleAppleWorkspace(private val project: Project) {
    private val reloadsQueue = BackgroundTaskQueue(project, LOADING_GRADLE_APPLE_PROJECT)

    init {
        ExternalProjectsManager.getInstance(project).runWhenInitialized { update() }
    }

    fun update() {
        // Skip the update if no Gradle projects are linked with this IDE project.
        if (GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) return

        reloadsQueue.run(object : Task.Backgroundable(project, LOADING_GRADLE_APPLE_PROJECT) {
            override fun run(indicator: ProgressIndicator) {
                updateOCWorkspace()
            }
        })
    }

    private fun updateOCWorkspace() {
        val workspace = OCWorkspace.getInstance(project).getModifiableModel(true)
        val compilerInfoSession = CompilerInfoCache().createSession<String>(ProgressIndicatorProvider.getGlobalProgressIndicator()!!)

        val messages = MultiMap<String, CompilerInfoCache.Message>()

        try {
            val environment = CidrToolEnvironment()
            val platform = checkNotNull(AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS_SIMULATOR)) { "iOS Simulator Platform not found" }
            val sdk = checkNotNull(platform.sdks.firstOrNull()) { "iOS Simulator SDK not found" }

            val alreadyAddedConfigurations = mutableSetOf<String>()
            AppleProjectDataService.forEachProject(project) { appleProject, moduleData, rootProjectPath ->
                for ((name, sourceSet) in appleProject.sourceSets) {
                    val buildConfig = "Debug"
                    // TODO Enable different variants (= build configurations, architectures, …)
                    val id = "${moduleData.id}:$name:$buildConfig"
                    assert(alreadyAddedConfigurations.add(id)) { "Duplicate configuration id" }

                    val config = workspace.addConfiguration(id, name, OCVariant(buildConfig))

                    // TODO Proper compiler detection and switch building
                    val compilerKind = OCCompilerKind.CLANG
                    val switches = CidrSwitchBuilder()
                        //.addSingleRaw("-arch").addSingleRaw(architectures.get(0))
                        .addSingleRaw("-isysroot").addSingleRaw(sdk.homePath)
                        .addSingleRaw("-fmodules") // Enable modules support (@import)

                    for (kind in OCLanguageKindProvider.getAllLanguageKinds()) {
                        if (kind.isObjC) switches.addSingleRaw("-fobjc-arc")

                        val langSettings = config.getLanguageCompilerSettings(kind)
                        langSettings.setCompiler(compilerKind, File("clang"), File(rootProjectPath))
                        langSettings.setCompilerSwitches(switches.build())
                    }

                    for (file in sourceSet.folderPaths.flatMap {
                        it.listFiles()?.asList() ?: emptyList()
                    }) { // don't flatten
                        if (OCFileTypeHelpers.isHeaderFile(file.name)) continue
                        val kind = // TODO Caution hack - should use AppCodeLanguageKindCalculatorHelper instead
                            OCLanguageKindProvider.getAllLanguageKinds().find { it.defaultSourceExtension.equals(file.extension, true) }
                                ?: OCLanguageKindCalculator.calculateMinimalKindByExtension(project, file.name)
                                ?: continue
                        config.addSource(VfsUtilCore.fileToUrl(file), kind)
                    }

                    compilerInfoSession.schedule(id, config, environment)
                }
            }

            compilerInfoSession.waitForAll(messages)
            workspace.preCommit()

            TransactionGuard.getInstance().submitTransactionAndWait {
                ApplicationManager.getApplication().runWriteAction {
                    if (project.isDisposed) {
                        workspace.dispose()
                        return@runWriteAction
                    }
                    workspace.commit()
                }
            }
        } finally {
            compilerInfoSession.dispose()
            workspace.dispose()
        }

        messages.values().forEach { each ->
            LOG.warn(each.getType().toString() + ": " + each.getText())
            // todo send messages to the build view (sync tab)
        }
    }

    companion object {
        private const val LOADING_GRADLE_APPLE_PROJECT = "Loading Gradle Apple Project..."
        private val LOG = Logger.getInstance(GradleAppleWorkspace::class.java)
        fun getInstance(project: Project): GradleAppleWorkspace = ServiceManager.getService(project, GradleAppleWorkspace::class.java)
    }
}