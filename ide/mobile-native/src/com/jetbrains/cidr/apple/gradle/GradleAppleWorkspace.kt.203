package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointer
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.apple.bridging.MobileKonanTarget
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileTypeHelpers
import com.jetbrains.cidr.lang.OCLanguageKindProvider
import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.lang.workspace.OCLanguageKindCalculator
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCVariant
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.swift.codeinsight.resolve.module.SwiftModuleIOCache
import org.jdom.Element
import org.jetbrains.konan.gradle.forEachKonanProject
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File

@State(name = "GradleAppleWorkspace", storages = [Storage("gradle.xml")])
class GradleAppleWorkspace(private val project: Project) : PersistentStateComponent<Element>, Disposable {
    private val reloadsQueue = BackgroundTaskQueue(project, LOADING_GRADLE_APPLE_PROJECT)
    private var disposable: Disposable = newDisposable()
    private var configurationData: Map<String, Data> = emptyMap()
    private var konanFrameworkTargets: Map<String, KonanTarget> = emptyMap()

    private class Data(val target: AppleTargetModel, disposable: Disposable) {
        val bridgingHeader: VirtualFilePointer? = target.bridgingHeader?.let {
            VirtualFilePointerManager.getInstance().create(VfsUtil.fileToUrl(it), disposable, null)
        }
    }

    init {
        ExternalProjectsManager.getInstance(project).runWhenInitialized { update() }
    }

    fun update() {
        // Skip the update if no Gradle projects are linked with this IDE project.
        if (GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) return

        reloadsQueue.run(object : Task.Backgroundable(project, LOADING_GRADLE_APPLE_PROJECT) {
            override fun run(indicator: ProgressIndicator) {
                SwiftModuleIOCache.getInstance() // initialize on a pooled thread
                updateOCWorkspace()
                project.messageBus.syncPublisher(GradleAppleWorkspaceListener.TOPIC).workspaceUpdated()

                configurationData.values.forEach { data ->
                    AppleXcodeProjectFileModificationListener.setUp(project, data.target, disposable)
                }
            }
        })
    }

    private fun newDisposable(): Disposable = Disposer.newDisposable(this, "GradleAppleWorkspaceState")

    override fun dispose(): Unit = Unit

    private fun updateOCWorkspace() {
        var committed = false
        val newDisposable = newDisposable()
        val configData = mutableMapOf<String, Data>()

        val workspace = OCWorkspace.getInstance(project).getModifiableModel(CLIENT_KEY, true)
        val compilerInfoSession = CompilerInfoCache().createSession<String>(ProgressIndicatorProvider.getGlobalProgressIndicator()!!)

        val messages = MultiMap<String, CompilerInfoCache.Message>()

        try {
            val environment = CidrToolEnvironment()
            val platform = checkNotNull(AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS_SIMULATOR)) { "iOS Simulator Platform not found" }
            val sdk = checkNotNull(platform.sdks.firstOrNull()) { "iOS Simulator SDK not found" }

            val alreadyAddedConfigurations = mutableSetOf<String>()
            AppleProjectDataService.forEachProject(project) { appleProject, moduleData, rootProjectPath ->
                for ((name, target) in appleProject.targets) {
                    val buildConfig = "Debug"
                    val buildVariant = OCVariant(buildConfig)
                    // TODO Enable different variants (= build configurations, architectures, …)
                    val id = "${moduleData.id}:$name:$buildConfig"
                    val testId = testConfigurationID(id)
                    assert(alreadyAddedConfigurations.add(id)) { "Duplicate configuration id" }

                    val config = workspace.addConfiguration(id, name, buildVariant)
                    val testConfig = workspace.addConfiguration(testId, "$name Tests", buildVariant)

                    // TODO Proper compiler detection and switch building
                    val compilerKind = OCCompilerKind.CLANG

                    fun configure(id: String, config: OCResolveConfiguration.ModifiableModel, directories: Set<File>) {
                        val data = Data(target, newDisposable)
                        configData[id] = data

                        for (kind in OCLanguageKindProvider.getAllLanguageKinds()) {
                            if (kind !is CLanguageKind) continue

                            val switches = CidrSwitchBuilder()
                                //.addSingleRaw("-arch").addSingleRaw(architectures.get(0))
                                .addSingleRaw("-isysroot").addSingleRaw(sdk.homePath)
                                .addSingleRaw("-fmodules") // Enable modules support (@import)
                            if (kind.isObjC) switches.addSingleRaw("-fobjc-arc")

                            val langSettings = config.getLanguageCompilerSettings(kind)
                            langSettings.setCompiler(compilerKind, File("clang"), File(rootProjectPath))
                            langSettings.setCompilerSwitches(switches.build())
                        }

                        for (file in directories.flatMap {
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

                    configure(id, config, target.sourceFolders)
                    configure(testId, testConfig, target.testFolders)
                }
            }

            // todo[florian.kistner] restrict to declared dependencies per apple target
            val frameworkTargets = mutableMapOf<String, KonanTarget>().apply {
                forEachKonanProject(project) { konanModel, module, _ ->
                    for (artifact in konanModel.artifacts) {
                        if (artifact.type != "FRAMEWORK") continue
                        val productModuleName = artifact.file.nameWithoutExtension
                        computeIfAbsent(productModuleName) { MobileKonanTarget(module.data.id, it) }
                    }
                }
            }

            compilerInfoSession.waitForAll(messages)
            workspace.preCommit()

            TransactionGuard.getInstance().submitTransactionAndWait {
                ApplicationManager.getApplication().runWriteAction {
                    if (project.isDisposed) {
                        workspace.dispose()
                        Disposer.dispose(newDisposable)
                        return@runWriteAction
                    }
                    workspace.commit()
                    synchronized(this) {
                        disposable = newDisposable
                        configurationData = configData
                        konanFrameworkTargets = frameworkTargets
                        committed = true
                    }
                }
            }
        } finally {
            compilerInfoSession.dispose()
            workspace.dispose()
            if (!committed) Disposer.dispose(newDisposable)
        }

        messages.values().forEach { each ->
            LOG.warn(each.getType().toString() + ": " + each.getText())
            // todo send messages to the build view (sync tab)
        }
    }

    override fun getState(): Element = Element("state").apply {
        for ((id, data) in configurationData) {
            addContent(Element("configuration").apply {
                setAttribute("id", id)
                setAttribute("targetName", data.target.name)
                data.target.bridgingHeader?.let { addContent(Element("bridgingHeader").addContent(it.path)) }
                for (sourceFolder in data.target.sourceFolders) {
                    addContent(Element("sourceFolder").addContent(sourceFolder.path))
                }
                for (testFolder in data.target.testFolders) {
                    addContent(Element("testFolder").addContent(testFolder.path))
                }
                setAttribute("editableXcodeProjectDir", data.target.editableXcodeProjectDir.path)
            })
        }
        for (target in konanFrameworkTargets.values) {
            addContent(Element("importedTarget").apply {
                setAttribute("id", target.moduleId)
                setAttribute("productModuleName", target.productModuleName)
            })
        }
    }

    override fun loadState(element: Element) {
        val newDisposable = newDisposable()
        val configData = mutableMapOf<String, Data>()
        val frameworkTargets = mutableMapOf<String, KonanTarget>()
        for (configElement in element.getChildren("configuration")) {
            configData[configElement.getAttributeValue("id") ?: continue] = Data(AppleTargetModelImpl(
                name = configElement.getAttributeValue("targetName") ?: continue,
                sourceFolders = configElement.getChildren("sourceFolder").mapTo(mutableSetOf()) { File(it.text) },
                testFolders = configElement.getChildren("testFolder").mapTo(mutableSetOf()) { File(it.text) },
                editableXcodeProjectDir = configElement.getAttributeValue("editableXcodeProjectDir")?.let { File(it) } ?: continue,
                bridgingHeader = configElement.getChild("bridgingHeader")?.let { File(it.text) }
            ), newDisposable)
        }
        for (configElement in element.getChildren("importedTarget")) {
            val productModuleName = configElement.getAttributeValue("productModuleName") ?: continue
            frameworkTargets[productModuleName] = MobileKonanTarget(
                configElement.getAttributeValue("id") ?: continue,
                productModuleName
            )
        }
        synchronized(this) {
            if (configurationData.isNotEmpty()) return Disposer.dispose(newDisposable)
            disposable = newDisposable
            configurationData = configData
            konanFrameworkTargets = frameworkTargets
        }
    }

    fun isOwnerOf(config: OCResolveConfiguration): Boolean = synchronized(this) {
        configurationData.containsKey(config.uniqueId)
    }

    fun getTarget(config: OCResolveConfiguration): AppleTargetModel? = synchronized(this) {
        configurationData[config.uniqueId]?.target
    }

    fun getTarget(name: String): AppleTargetModel? = synchronized(this) {
        for (data in configurationData.values) {
            return@synchronized data.target.takeIf { it.name == name } ?: continue
        }
        null
    }

    val targets: List<AppleTargetModel>
        get() = synchronized(this) {
            configurationData.values.map { it.target }
        }

    fun getConfiguration(targetName: String) = synchronized(this) {
        for ((id, data) in configurationData) {
            if (data.target.name == targetName) return@synchronized OCWorkspace.getInstance(project).getConfigurationById(id)
        }
        null
    }

    fun getBridgingHeader(config: OCResolveConfiguration): VirtualFile? = synchronized(this) {
        configurationData[config.uniqueId]?.bridgingHeader?.file
    }

    val availableKonanFrameworkTargets: Map<String, KonanTarget>
        get() = synchronized(this) { konanFrameworkTargets }

    companion object {
        private const val CLIENT_KEY = "GradleApple"
        private const val LOADING_GRADLE_APPLE_PROJECT = "Loading Gradle Apple Project..."
        private val LOG = Logger.getInstance(GradleAppleWorkspace::class.java)
        fun getInstance(project: Project): GradleAppleWorkspace = ServiceManager.getService(project, GradleAppleWorkspace::class.java)

        fun testConfigurationID(sourceConfigurationID: String): String = "$sourceConfigurationID:test"
        fun sourceConfigurationID(testConfigurationID: String): String? =
            if (testConfigurationID.endsWith(":test"))
                testConfigurationID.removeSuffix(":test")
            else null
    }
}