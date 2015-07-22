/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.HashMap
import com.intellij.util.containers.HashSet
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.util.ArrayList

public object JarUserDataIndex : FileBasedIndexExtension<String, String>() {
    private val INDEX_TIME_STAMP_KEY = Key.create<Long>("INDEX_TIME_STAMP_KEY")

    private val name = ID.create<String, String>(JarUserDataIndex::class.qualifiedName)
    private val collectors: MutableList<JarUserDataCollector<*>> = ArrayList()

    private val descriptor = EnumeratorStringDescriptor()

    // Sdk list can be outdated if some new jdks are added
    // TODO: Subscribe to sdk table change
    val allJDKRoots = ProjectJdkTable.getInstance().getAllJdks().flatMapTo(HashSet<VirtualFile>()) { jdk ->
        jdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() +
        jdk.rootProvider.getFiles(OrderRootType.SOURCES).toList()
    }

    private val inputFilter = FileBasedIndex.InputFilter() {
        file: VirtualFile -> file.isInLocalFileSystem && file.extension == "jar"
    }

    private val indexer = DataIndexer<String, String, FileContent>() { inputData: FileContent ->
        val localJarFile = inputData.getFile()
        val jarFile = JarFileSystemUtil.findJarRootByLocal(inputData.getFile()) ?: return@DataIndexer mapOf()

        val isSdk = VfsUtilCore.isUnder(jarFile, allJDKRoots)

        val resultMap = HashMap<String, String>()
        resultMap[INDEX_TIME_STAMP_KEY.toString()] = localJarFile.timeStamp.toString()

        collectors.forEach { collector ->
            jarFile.putUserData(collector.key, null)

            val keyName = collector.key.toString()

            resultMap[keyName] = if (!isSdk) {
                @suppress("UNCHECKED_CAST")
                countForCollector(collector as JarUserDataCollector<Any>, jarFile).toString()
            }
            else {
                collector.sdk.toString()
            }
        }

        resultMap
    }

    override fun getName(): ID<String, String> = name

    override fun getVersion(): Int = 2

    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter() = inputFilter
    override fun getIndexer() = indexer

    override fun getKeyDescriptor(): KeyDescriptor<String> = descriptor
    override fun getValueExternalizer(): DataExternalizer<String> = descriptor

    public fun register(collector: JarUserDataCollector<*>) {
        collectors.add(collector)
    }

    public fun <T : Any> getValue(collector: JarUserDataCollector<T>, inJarFile: VirtualFile): T? {
        val jarFile = JarFileSystemUtil.findJarFileRoot(inJarFile) ?: return null
        val localJarFile = JarFileSystemUtil.findLocalJarFile(jarFile) ?: return null

        val kotlinState = localJarFile.getUserData(collector.key)
        if (kotlinState != null) {
            val timeStamp = localJarFile.getUserData(INDEX_TIME_STAMP_KEY)

            if (localJarFile.timeStamp != timeStamp) {
                // Temporary ignore index value while index will not get an update
                if (isDumbMode()) return null

                val hasValueInIndex = runReadAction {
                    FileBasedIndex.getInstance().getValues(name, collector.key.toString(),
                                                           GlobalSearchScope.FilesScope(null, listOf(localJarFile)))
                }.isNotEmpty()

                if (!hasValueInIndex) {
                    // As there's no value in index, manually remove the value and request refresh
                    storeResult(collector, localJarFile, null, null)
                }

                return null
            }

            return kotlinState
        }

        if (VfsUtilCore.isUnder(jarFile, allJDKRoots)) {
            storeResult(collector, localJarFile, collector.sdk, localJarFile.timeStamp)
            return collector.sdk
        }

        if (isDumbMode()) return null

        val filesScope = GlobalSearchScope.FilesScope(null, listOf(localJarFile))

        val collectorValues = runReadAction { FileBasedIndex.getInstance().getValues(name, collector.key.toString(), filesScope) }
        val timeStampValues = runReadAction {
            FileBasedIndex.getInstance().getValues(name, INDEX_TIME_STAMP_KEY.toString(), filesScope)
        }

        if (collectorValues.isEmpty() || timeStampValues.isEmpty()) {
            scheduleJarProcessing(collector, jarFile, localJarFile)

            return null
        }

        val value = collectorValues.single()

        val state = collector.state(value)
        val timeStamp = java.lang.Long.parseLong(timeStampValues.first())

        storeResult(collector, localJarFile, state, timeStamp)

        return state
    }

    private fun <T: Any> scheduleJarProcessing(collector: JarUserDataCollector<T>, jarFile: VirtualFile, localJarFile: VirtualFile) {
        if (localJarFile.getUserData(collector.key) != null) return

        storeResult(collector, localJarFile, collector.init, localJarFile.timeStamp)

        ApplicationManager.getApplication().executeOnPooledThread {
            runReadAction {
                val result = countForCollector(collector, jarFile)
                storeResult(collector, localJarFile, result, localJarFile.timeStamp)
            }
        }
    }

    public fun isDumbMode(): Boolean {
        return ProjectManager.getInstance().getOpenProjects().any {
            project ->
            DumbService.getInstance(project).isDumb()
        }
    }

    private fun <T : Any> countForCollector(collector: JarUserDataCollector<T>, jarFile: VirtualFile): T {
        var result = collector.notFoundState

        VfsUtilCore.processFilesRecursively(jarFile) { file ->
            if (collector.count(file) == collector.stopState) {
                result = collector.stopState

                // stop processing
                false
            }
            else {
                // continue processing
                true
            }
        }

        return result
    }

    private fun <T : Any> storeResult(collector: JarUserDataCollector<T>, localJarFile: VirtualFile, result: T?, timeStamp: Long?) {
        assert(localJarFile.isInLocalFileSystem)

        localJarFile.putUserData(INDEX_TIME_STAMP_KEY, timeStamp)
        localJarFile.putUserData(collector.key, result)
    }

    object JarFileSystemUtil {
        public fun findJarFileRoot(inJarFile: VirtualFile): VirtualFile? {
            if (!inJarFile.getUrl().startsWith("jar://")) return null

            var jarFile = inJarFile
            while (jarFile.getParent() != null) jarFile = jarFile.getParent()

            return jarFile
        }

        public fun findJarRootByLocal(file: VirtualFile): VirtualFile? {
            assert(file.isInLocalFileSystem)

            return StandardFileSystems.jar().findFileByPath(file.getPath() + URLUtil.JAR_SEPARATOR)
        }

        public fun findLocalJarFile(jarFile: VirtualFile): VirtualFile? {
            assert(!jarFile.isInLocalFileSystem)

            val path = jarFile.getPath()

            val jarSeparatorIndex = path.indexOf(URLUtil.JAR_SEPARATOR)
            assert(jarSeparatorIndex >= 0) { "Path passed to JarFileSystem must have jar separator '!/': $path" }
            val localPath = path.substring(0, jarSeparatorIndex)

            return StandardFileSystems.local().findFileByPath(localPath)
        }
    }

    interface JarUserDataCollector<State> {
        val key: Key<State>

        val init: State
        val stopState: State
        val notFoundState: State

        val sdk: State

        fun count(file: VirtualFile): State
        fun state(str: String): State
    }
}
