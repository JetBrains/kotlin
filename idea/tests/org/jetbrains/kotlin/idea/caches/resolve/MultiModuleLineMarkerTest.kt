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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class MultiModuleLineMarkerTest : AbstractMultiModuleHighlightingTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleLineMarker/"

    override val shouldCheckLineMarkers = true

    override val shouldCheckResult = false

    override fun doTestLineMarkers() = true

    fun testFromCommonToJvmHeader() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testFromCommonToJvmImpl() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testFromClassToAlias() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testWithOverloads() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    }

    fun testSuspendImplInPlatformModules() {
        doMultiPlatformTest(TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], TargetPlatformKind.JavaScript)
    }

    fun testKotlinTestAnnotations() {
        doMultiPlatformTest(TargetPlatformKind.JavaScript,
                            configureModule = { module, _ ->
                                ModuleRootModificationUtil.updateModel(module) {
                                    with(it.getModuleExtension(CompilerModuleExtension::class.java)!!) {
                                        inheritCompilerOutputPath(false)
                                        setCompilerOutputPathForTests("js_out")
                                    }
                                }
                            })
    }
}