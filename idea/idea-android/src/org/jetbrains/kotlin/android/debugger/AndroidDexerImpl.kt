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

package org.jetbrains.kotlin.android.debugger

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.sdk.AndroidSdkData
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.AndroidDexer
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad
import java.io.File
import java.net.URLClassLoader
import java.security.ProtectionDomain

class AndroidDexerImpl(val project: Project) : AndroidDexer {
    private val cachedDexWrapper = CachedValuesManager.getManager(project).createCachedValue({
        val dexWrapper = doGetAndroidDexFile()?.let { dexJarFile ->
            val androidDexWrapperName = AndroidDexWrapper::class.java.canonicalName
            val classBytes = this.javaClass.classLoader.getResource(
                    androidDexWrapperName.replace('.', '/') + ".class").readBytes()

            val dexClassLoader = object : URLClassLoader(arrayOf(dexJarFile.toURI().toURL()), this::class.java.classLoader) {
                init {
                    defineClass(androidDexWrapperName, classBytes, 0, classBytes.size, null as ProtectionDomain?)
                }
            }

            Class.forName(androidDexWrapperName, true, dexClassLoader).newInstance()
        }

        CachedValueProvider.Result.createSingleDependency(dexWrapper, ProjectRootModificationTracker.getInstance(project))
    }, /* trackValue = */ false)

    override fun dex(classes: Collection<ClassToLoad>): ByteArray? {
        val dexWrapper = cachedDexWrapper.value
        val dexMethod = dexWrapper::class.java.methods.firstOrNull { it.name == "dex" } ?: return null
        return dexMethod.invoke(dexWrapper, classes) as? ByteArray ?: return null
    }

    private fun doGetAndroidDexFile(): File? {
        for (module in ModuleManager.getInstance(project).modules) {
            val androidFacet = AndroidFacet.getInstance(module) ?: continue
            val sdkData = AndroidSdkData.getSdkData(androidFacet) ?: continue
            val latestBuildTool = sdkData.getLatestBuildTool(/* allowPreview = */ false)
                                  ?: sdkData.getLatestBuildTool(/* allowPreview = */ true)
                                  ?: continue

            val dxJar = File(latestBuildTool.location, "lib/dx.jar")
            if (dxJar.exists()) {
                return dxJar
            }
        }

        return null
    }
}