/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.diagnostic.PluginException
import com.intellij.execution.console.IdeConsoleRootType
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.PathUtilEx
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.getScriptRelatedModuleInfo
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.scripting.definitions.*
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.concurrent.write
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStdlib
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class ScriptDefinitionsManager(private val project: Project) : LazyScriptDefinitionProvider() {
    private var definitionsBySource = mutableMapOf<ScriptDefinitionsSource, List<ScriptDefinition>>()
    private var definitions: List<ScriptDefinition>? = null

    private val failedContributorsHashes = HashSet<Int>()

    private val scriptDefinitionsCacheLock = ReentrantLock()
    private val scriptDefinitionsCache = SLRUMap<File, ScriptDefinition>(10, 10)

    override fun findDefinition(file: File): ScriptDefinition? {
        if (nonScriptFileName(file.name)) return null
        if (!isReady()) return null

        val cached = scriptDefinitionsCacheLock.withLock { scriptDefinitionsCache.get(file) }
        if (cached != null) return cached

        val virtualFile = VfsUtil.findFileByIoFile(file, true)
        val definition =
            if (virtualFile != null && ScratchFileService.getInstance().getRootType(virtualFile) is ScratchRootType) {
                // Scratch should always have default script definition
                getDefaultDefinition()
            } else {
                super.findDefinition(file) ?: return null
            }

        scriptDefinitionsCacheLock.withLock {
            scriptDefinitionsCache.put(file, definition)
        }

        return definition
    }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? = findDefinition(File(fileName))?.legacyDefinition

    fun reloadDefinitionsBy(source: ScriptDefinitionsSource) = lock.write {
        if (definitions == null) return // not loaded yet

        if (source !in definitionsBySource) error("Unknown script definition source: $source")

        definitionsBySource[source] = source.safeGetDefinitions()

        definitions = definitionsBySource.values.flattenTo(mutableListOf())

        updateDefinitions()
    }

    override val currentDefinitions
        get() =
            (definitions ?: kotlin.run {
                reloadScriptDefinitions()
                definitions!!
            }).asSequence().filter { KotlinScriptingSettings.getInstance(project).isScriptDefinitionEnabled(it) }

    private fun getSources(): List<ScriptDefinitionsSource> {
        @Suppress("DEPRECATION")
        val fromDeprecatedEP = Extensions.getArea(project).getExtensionPoint(ScriptTemplatesProvider.EP_NAME).extensions.toList()
            .map { ScriptTemplatesProviderAdapter(it).asSource() }
        val fromNewEp = Extensions.getArea(project).getExtensionPoint(ScriptDefinitionContributor.EP_NAME).extensions.toList()
            .map { it.asSource() }
        return fromNewEp.dropLast(1) + fromDeprecatedEP + fromNewEp.last()
    }

    fun reloadScriptDefinitions() = lock.write {
        for (source in getSources()) {
            val definitions = source.safeGetDefinitions()
            definitionsBySource[source] = definitions
        }

        definitions = definitionsBySource.values.flattenTo(mutableListOf())

        updateDefinitions()
    }

    fun reorderScriptDefinitions() = lock.write {
        updateDefinitions()
    }

    fun getAllDefinitions(): List<ScriptDefinition> {
        return definitions ?: kotlin.run {
            reloadScriptDefinitions()
            definitions!!
        }
    }

    fun isReady(): Boolean {
        if (definitions == null) {
            reloadScriptDefinitions()
        }
        return definitions != null && definitionsBySource.keys.all { source ->
            // TODO: implement another API for readiness checking
            (source as? ScriptDefinitionContributor)?.isReady() != false
        }
    }

    override fun getDefaultDefinition(): ScriptDefinition {
        val standardScriptDefinitionContributor = ScriptDefinitionContributor.find<StandardScriptDefinitionContributor>(project)
            ?: error("StandardScriptDefinitionContributor should be registered is plugin.xml")
        return ScriptDefinition.FromLegacy(getScriptingHostConfiguration(), standardScriptDefinitionContributor.getDefinitions().last())
    }

    private fun updateDefinitions() {
        assert(lock.isWriteLocked) { "updateDefinitions should only be called under the write lock" }

        definitions = definitions?.sortedBy {
            KotlinScriptingSettings.getInstance(project).getScriptDefinitionOrder(it)
        }

        val fileTypeManager = FileTypeManager.getInstance()

        val newExtensions = getKnownFilenameExtensions().filter {
            fileTypeManager.getFileTypeByExtension(it) != KotlinFileType.INSTANCE
        }.toList()

        if (newExtensions.any()) {
            // Register new file extensions
            ApplicationManager.getApplication().invokeLater {
                runWriteAction {
                    newExtensions.forEach {
                        fileTypeManager.associateExtension(KotlinFileType.INSTANCE, it)
                    }
                }
            }
        }

        clearCache()
        scriptDefinitionsCacheLock.withLock { scriptDefinitionsCache.clear() }

        // TODO: clear by script type/definition
        ScriptConfigurationManager.getInstance(project).clearConfigurationCachesAndRehighlight()
    }

    private fun ScriptDefinitionsSource.safeGetDefinitions(): List<ScriptDefinition> {
        if (!failedContributorsHashes.contains(this@safeGetDefinitions.hashCode())) try {
            return definitions.toList()
        } catch (t: Throwable) {
            if (t is ControlFlowException) throw t
            // reporting failed loading only once
            LOG.error("[kts] cannot load script definitions using $this", t)
            failedContributorsHashes.add(this@safeGetDefinitions.hashCode())
        }
        return emptyList()
    }

    companion object {
        fun getInstance(project: Project): ScriptDefinitionsManager =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java) as ScriptDefinitionsManager
    }
}

// TODO: consider rewriting to return sequence
fun loadDefinitionsFromTemplates(
    templateClassNames: List<String>,
    templateClasspath: List<File>,
    baseHostConfiguration: ScriptingHostConfiguration,
    // TODO: need to provide a way to specify this in compiler/repl .. etc
    /*
     * Allows to specify additional jars needed for DependenciesResolver (and not script template).
     * Script template dependencies naturally become (part of) dependencies of the script which is not always desired for resolver dependencies.
     * i.e. gradle resolver may depend on some jars that 'built.gradle.kts' files should not depend on.
     */
    additionalResolverClasspath: List<File> = emptyList()
): List<ScriptDefinition> {
    val classpath = templateClasspath + additionalResolverClasspath
    LOG.info("[kts] loading script definitions $templateClassNames using cp: ${classpath.joinToString(File.pathSeparator)}")
    val baseLoader = ScriptDefinitionContributor::class.java.classLoader
    val loader = if (classpath.isEmpty()) baseLoader else URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseLoader)

    return templateClassNames.mapNotNull { templateClassName ->
        try {
            // TODO: drop class loading here - it should be handled downstream
            // as a compatibility measure, the asm based reading of annotations should be implemented to filter classes before classloading
            val template = loader.loadClass(templateClassName).kotlin
            val hostConfiguration = ScriptingHostConfiguration(baseHostConfiguration) {
                configurationDependencies(JvmDependency(templateClasspath))
            }
            when {
                template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>() != null -> {
                    ScriptDefinition.FromLegacyTemplate(hostConfiguration, template, templateClasspath)
                }
                template.annotations.firstIsInstanceOrNull<kotlin.script.experimental.annotations.KotlinScript>() != null -> {
                    ScriptDefinition.FromTemplate(hostConfiguration, template, ScriptDefinition::class)
                }
                else -> {
                    LOG.warn("[kts] cannot find a valid script definition annotation on the class $template")
                    null
                }
            }
        } catch (e: ClassNotFoundException) {
            // Assuming that direct ClassNotFoundException is the result of versions mismatch and missing subsystems, e.g. gradle
            // so, it only results in warning, while other errors are severe misconfigurations, resulting it user-visible error
            LOG.warn("[kts] cannot load script definition class $templateClassName")
            null
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e

            val message = "[kts] cannot load script definition class $templateClassName"
            val thirdPartyPlugin = PluginManagerCore.getPluginByClassName(templateClassName)
            if (thirdPartyPlugin != null) {
                LOG.error(PluginException(message, e, thirdPartyPlugin))
            } else {
                LOG.error(message, e)
            }
            null
        }
    }
}

@Deprecated("migrating to new configuration refinement: use ScriptDefinitionsSource internally and kotlin.script.experimental.intellij.ScriptDefinitionsProvider as a providing extension point")
interface ScriptDefinitionContributor {

    @Deprecated("migrating to new configuration refinement: drop usages")
    val id: String

    @Deprecated("migrating to new configuration refinement: use ScriptDefinitionsSource instead")
    fun getDefinitions(): List<KotlinScriptDefinition>

    @JvmDefault
    @Deprecated("migrating to new configuration refinement: drop usages")
    fun isReady() = true

    companion object {
        val EP_NAME: ExtensionPointName<ScriptDefinitionContributor> =
            ExtensionPointName.create<ScriptDefinitionContributor>("org.jetbrains.kotlin.scriptDefinitionContributor")

        inline fun <reified T> find(project: Project) =
            Extensions.getArea(project).getExtensionPoint(EP_NAME).extensions.filterIsInstance<T>().firstOrNull()
    }
}

@Deprecated("migrating to new configuration refinement: use ScriptDefinitionsSource directly instead")
interface ScriptDefinitionSourceAsContributor : ScriptDefinitionContributor, ScriptDefinitionsSource {

    override fun getDefinitions(): List<KotlinScriptDefinition> = definitions.map { it.legacyDefinition }.toList()
}

@Deprecated("migrating to new configuration refinement: convert all contributors to ScriptDefinitionsSource/ScriptDefinitionsProvider")
class ScriptDefinitionSourceFromContributor(
    val contributor: ScriptDefinitionContributor,
    val hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration
) : ScriptDefinitionsSource {
    override val definitions: Sequence<ScriptDefinition>
        get() =
            if (contributor is ScriptDefinitionsSource) contributor.definitions
            else contributor.getDefinitions().asSequence().map { ScriptDefinition.FromLegacy(hostConfiguration, it) }
}

fun ScriptDefinitionContributor.asSource(): ScriptDefinitionsSource =
    if (this is ScriptDefinitionsSource) this
    else ScriptDefinitionSourceFromContributor(this)

class StandardScriptDefinitionContributor(project: Project) : ScriptDefinitionContributor {
    private val standardIdeScriptDefinition = StandardIdeScriptDefinition(project)

    override fun getDefinitions() = listOf(standardIdeScriptDefinition)

    override val id: String = "StandardKotlinScript"
}


class StandardIdeScriptDefinition internal constructor(project: Project) : KotlinScriptDefinition(ScriptTemplateWithArgs::class) {
    override val dependencyResolver = BundledKotlinScriptDependenciesResolver(project)
}

class BundledKotlinScriptDependenciesResolver(private val project: Project) : DependenciesResolver {
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment
    ): DependenciesResolver.ResolveResult {
        val virtualFile = scriptContents.file?.let { VfsUtil.findFileByIoFile(it, true) }

        val javaHome = getScriptSDK(project, virtualFile)

        var classpath = with(PathUtil.kotlinPathsForIdeaPlugin) {
            listOf(reflectPath, stdlibPath, scriptRuntimePath)
        }
        if (ScratchFileService.getInstance().getRootType(virtualFile) is IdeConsoleRootType) {
            classpath = scriptCompilationClasspathFromContextOrStdlib(wholeClasspath = true) + classpath
        }

        return ScriptDependencies(javaHome = javaHome?.let(::File), classpath = classpath).asSuccess()
    }

    private fun getScriptSDK(project: Project, virtualFile: VirtualFile?): String? {
        if (virtualFile != null) {
            val dependentModuleSourceInfo = getScriptRelatedModuleInfo(project, virtualFile)
            val sdk = dependentModuleSourceInfo?.dependencies()?.filterIsInstance<SdkInfo>()?.singleOrNull()?.sdk
            if (sdk != null) {
                return sdk.homePath
            }
        }

        val jdk = ProjectRootManager.getInstance(project).projectSdk
            ?: ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk -> sdk.sdkType is JavaSdk }
            ?: PathUtilEx.getAnyJdk(project)
        return jdk?.homePath
    }
}
