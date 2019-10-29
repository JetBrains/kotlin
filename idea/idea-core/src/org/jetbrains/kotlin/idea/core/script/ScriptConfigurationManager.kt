/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.configuration.AbstractScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult

// NOTE: this service exists exclusively because ScriptDependencyManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptDependenciesProvider(project: Project) : ScriptDependenciesProvider(project) {
    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? {
        val configuration = getScriptConfiguration(file)
        val reports = IdeScriptReportSink.getReports(file)
        if (configuration == null && reports.isNotEmpty()) {
            return makeFailureResult(reports)
        }
        return configuration?.asSuccess(reports)
    }

    override fun getScriptConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        return ScriptConfigurationManager.getInstance(project).getConfiguration(file)
    }

}

/**
 * Facade for loading and caching Kotlin script files configuration.
 *
 * This service also starts indexing of new dependency roots and runs highlighting
 * of opened files when configuration will be loaded or updated.
 */
interface ScriptConfigurationManager {
    /**
     * Get cached configuration for [file] or load it.
     * May return null even configuration was loaded but was not yet applied.
     */
    fun getConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper?

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile>

    /**
     * @see [getConfiguration]
     */
    fun getScriptClasspath(file: KtFile): List<VirtualFile>

    /**
     * Check if configuration is already cached for [file] (in cache or FileAttributes).
     * The result may be true, even cached configuration is considered out-of-date.
     *
     * Supposed to be used to switch highlighting off for scripts without configuration
     * to avoid all file being highlighted in red.
     */
    fun hasConfiguration(file: KtFile): Boolean

    /**
     * See [ScriptConfigurationUpdater].
     */
    val updater: ScriptConfigurationUpdater

    /**
     * Clear all caches and re-highlighting opened scripts
     */
    fun clearConfigurationCachesAndRehighlight()

    /**
     * Save configurations into cache.
     * Start indexing for new class/source roots.
     * Re-highlight opened scripts with changed configuration.
     */
    fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptConfigurationSnapshot>>)

    ///////////////
    // classpath roots info:

    fun getScriptSdk(file: VirtualFile): Sdk?
    fun getFirstScriptsSdk(): Sdk?

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope

    fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope
    fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope
    fun getAllScriptsDependenciesClassFiles(): List<VirtualFile>
    fun getAllScriptDependenciesSources(): List<VirtualFile>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScriptConfigurationManager =
            ServiceManager.getService(project, ScriptConfigurationManager::class.java)

        fun getScriptDefaultSdk(project: Project): Sdk? {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }
            if (projectSdk != null) return projectSdk

            val anyJavaSdk = getAllProjectSdks().find { it.canBeUsedForScript() }
            if (anyJavaSdk != null) {
                return anyJavaSdk
            }

            LOG.warn(
                "Default Script SDK is null: " +
                        "projectSdk = ${ProjectRootManager.getInstance(project).projectSdk}, " +
                        "all sdks = ${getAllProjectSdks().joinToString("\n")}"
            )
            return null
        }

        fun toVfsRoots(roots: Iterable<File>): List<VirtualFile> {
            return roots.mapNotNull { it.classpathEntryToVfs() }
        }

        private fun Sdk.canBeUsedForScript() = sdkType is JavaSdkType

        private fun File.classpathEntryToVfs(): VirtualFile? {
            val res = when {
                !exists() -> null
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath)
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        @TestOnly
        fun updateScriptDependenciesSynchronously(file: PsiFile, project: Project) {
            (getInstance(project) as AbstractScriptConfigurationManager).updateScriptDependenciesSynchronously(file)
        }
    }
}
