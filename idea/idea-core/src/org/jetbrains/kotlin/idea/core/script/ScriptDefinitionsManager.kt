/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.execution.console.IdeConsoleRootType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
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
import com.intellij.ui.EditorNotifications
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.getScriptRelatedModuleInfo
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.scripting.shared.definitions.KotlinScriptDefinitionAdapterFromNewAPI
import org.jetbrains.kotlin.scripting.shared.definitions.LazyScriptDefinitionProvider
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.createCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStdlib
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class ScriptDefinitionsManager(private val project: Project) : LazyScriptDefinitionProvider() {
    private var definitionsByContributor = mutableMapOf<ScriptDefinitionContributor, List<KotlinScriptDefinition>>()
    private var definitions: List<KotlinScriptDefinition>? = null

    private val failedContributorsHashes = HashSet<Int>()

    private val scriptDefinitionsCacheLock = ReentrantReadWriteLock()
    private val scriptDefinitionsCache = SLRUMap<String, KotlinScriptDefinition>(10, 10)

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? {
        if (nonScriptFileName(fileName)) return null
        if (!isReady()) return null

        val cached = synchronized(scriptDefinitionsCacheLock) { scriptDefinitionsCache.get(fileName) }
        if (cached != null) return cached

        val definition = super.findScriptDefinition(fileName) ?: return null

        synchronized(scriptDefinitionsCacheLock) {
            scriptDefinitionsCache.put(fileName, definition)
        }

        return definition
    }

    fun reloadDefinitionsBy(contributor: ScriptDefinitionContributor) = lock.write {
        if (definitions == null) return // not loaded yet

        if (contributor !in definitionsByContributor) error("Unknown contributor: ${contributor.id}")

        definitionsByContributor[contributor] = contributor.safeGetDefinitions()

        definitions = definitionsByContributor.values.flattenTo(mutableListOf())

        updateDefinitions()
    }

    fun getDefinitionsBy(contributor: ScriptDefinitionContributor): List<KotlinScriptDefinition> = lock.write {
        if (definitions == null) return emptyList() // not loaded yet

        if (contributor !in definitionsByContributor) error("Unknown contributor: ${contributor.id}")

        return definitionsByContributor[contributor] ?: emptyList()
    }

    override val currentDefinitions: Sequence<KotlinScriptDefinition>
        get() =
            (definitions ?: kotlin.run {
                reloadScriptDefinitions()
                definitions!!
            }).asSequence().filter { KotlinScriptingSettings.getInstance(project).isScriptDefinitionEnabled(it) }

    private fun getContributors(): List<ScriptDefinitionContributor> {
        @Suppress("DEPRECATION")
        val fromDeprecatedEP = Extensions.getArea(project).getExtensionPoint(ScriptTemplatesProvider.EP_NAME).extensions.toList()
            .map(::ScriptTemplatesProviderAdapter)
        val fromNewEp = Extensions.getArea(project).getExtensionPoint(ScriptDefinitionContributor.EP_NAME).extensions.toList()
        return fromNewEp.dropLast(1) + fromDeprecatedEP + fromNewEp.last()
    }

    fun reloadScriptDefinitions() = lock.write {
        for (contributor in getContributors()) {
            val definitions = contributor.safeGetDefinitions()
            definitionsByContributor[contributor] = definitions
        }

        definitions = definitionsByContributor.values.flattenTo(mutableListOf())

        updateDefinitions()
    }

    fun reorderScriptDefinitions() = lock.write {
        updateDefinitions()
    }

    fun getAllDefinitions(): List<KotlinScriptDefinition> {
        return definitions ?: kotlin.run {
            reloadScriptDefinitions()
            definitions!!
        }
    }

    fun isReady(): Boolean {
        return definitionsByContributor.keys.all { contributor ->
            contributor.isReady()
        }
    }

    override fun getDefaultScriptDefinition(): KotlinScriptDefinition {
        val standardScriptDefinitionContributor = ScriptDefinitionContributor.find<StandardScriptDefinitionContributor>(project)
            ?: error("StandardScriptDefinitionContributor should be registered is plugin.xml")
        return standardScriptDefinitionContributor.getDefinitions().last()
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
        scriptDefinitionsCache.clear()

        // TODO: clear by script type/definition
        ServiceManager.getService(project, ScriptDependenciesCache::class.java).clear()

        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }

    private fun ScriptDefinitionContributor.safeGetDefinitions(): List<KotlinScriptDefinition> {
        if (!failedContributorsHashes.contains(this@safeGetDefinitions.hashCode())) try {
            return getDefinitions()
        } catch (t: Throwable) {
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


private val LOG = Logger.getInstance("ScriptTemplatesProviders")

fun loadDefinitionsFromTemplates(
    templateClassNames: List<String>,
    templateClasspath: List<File>,
    environment: Environment = emptyMap(),
    // TODO: need to provide a way to specify this in compiler/repl .. etc
    /*
     * Allows to specify additional jars needed for DependenciesResolver (and not script template).
     * Script template dependencies naturally become (part of) dependencies of the script which is not always desired for resolver dependencies.
     * i.e. gradle resolver may depend on some jars that 'built.gradle.kts' files should not depend on.
     */
    additionalResolverClasspath: List<File> = emptyList()
): List<KotlinScriptDefinition> {
    val classpath = templateClasspath + additionalResolverClasspath
    LOG.info("[kts] loading script definitions $templateClassNames using cp: ${classpath.joinToString(File.pathSeparator)}")
    val baseLoader = ScriptDefinitionContributor::class.java.classLoader
    val loader = if (classpath.isEmpty()) baseLoader else URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseLoader)

    return templateClassNames.mapNotNull { templateClassName ->
        try {
            // TODO: drop class loading here - it should be handled downstream
            // as a compatibility measure, the asm based reading of annotations should be implemented to filter classes before classloading
            val template = loader.loadClass(templateClassName).kotlin
            when {
                template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.ScriptTemplateDefinition>() != null ||
                        template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>() != null -> {
                    KotlinScriptDefinitionFromAnnotatedTemplate(
                        template,
                        environment,
                        templateClasspath
                    )
                }
                template.annotations.firstIsInstanceOrNull<kotlin.script.experimental.annotations.KotlinScript>() != null -> {
                    val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                        configurationDependencies(JvmDependency(classpath))
                    }
                    KotlinScriptDefinitionAdapterFromNewAPI(
                        createCompilationConfigurationFromTemplate(
                            KotlinType(
                                template
                            ), hostConfiguration, KotlinScriptDefinition::class
                        ),
                        hostConfiguration
                    )
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
            LOG.error("[kts] cannot load script definition class $templateClassName", e)
            null
        }
    }
}

interface ScriptDefinitionContributor {
    val id: String

    fun getDefinitions(): List<KotlinScriptDefinition>
    fun isReady() = true

    companion object {
        val EP_NAME: ExtensionPointName<ScriptDefinitionContributor> =
            ExtensionPointName.create<ScriptDefinitionContributor>("org.jetbrains.kotlin.scriptDefinitionContributor")

        inline fun <reified T> find(project: Project) =
            Extensions.getArea(project).getExtensionPoint(ScriptDefinitionContributor.EP_NAME).extensions.filterIsInstance<T>().firstOrNull()
    }

}

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
