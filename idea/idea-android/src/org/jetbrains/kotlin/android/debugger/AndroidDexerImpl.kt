/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    private val cachedDexWrapper = CachedValuesManager.getManager(project).createCachedValue(
        {
            val dexWrapper = doGetAndroidDexFile()?.let { dexJarFile ->
                val androidDexWrapperName = AndroidDexWrapper::class.java.canonicalName
                val classBytes = this.javaClass.classLoader.getResource(
                    androidDexWrapperName.replace('.', '/') + ".class"
                ).readBytes()

                val dexClassLoader = object : URLClassLoader(arrayOf(dexJarFile.toURI().toURL()), this::class.java.classLoader) {
                    init {
                        defineClass(androidDexWrapperName, classBytes, 0, classBytes.size, null as ProtectionDomain?)
                    }
                }

                Class.forName(androidDexWrapperName, true, dexClassLoader).newInstance()
            }

            CachedValueProvider.Result.createSingleDependency(dexWrapper, ProjectRootModificationTracker.getInstance(project))
        }, /* trackValue = */ false
    )

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