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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.util.*

fun <LP : LibraryProperties<out Any>> isDetected(provider: LibraryPresentationProvider<LP>, library: Library): Boolean {
    return getLibraryProperties(provider, library) != null
}

fun <LP : LibraryProperties<out Any>> getLibraryProperties(provider: LibraryPresentationProvider<LP>, library: Library): LP? {
    if (isExternalLibrary(library)) return null
    return provider.detect(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)))
}

private val MAVEN_SYSTEM_ID = ProjectSystemId("MAVEN")
val GRADLE_SYSTEM_ID = ProjectSystemId("GRADLE")

private fun isExternalLibrary(library: Library): Boolean {
    return ExternalSystemApiUtil.isExternalSystemLibrary(library, ProjectSystemId.IDE) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, GRADLE_SYSTEM_ID) ||
           ExternalSystemApiUtil.isExternalSystemLibrary(library, MAVEN_SYSTEM_ID)
}

fun getRuntimeJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return JavaRuntimeDetectionUtil.getRuntimeJar(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)))
}

fun getReflectJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return LibraryUtils.getJarFile(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)), PathUtil.KOTLIN_JAVA_REFLECT_JAR)
}

fun getRuntimeSrcJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return getRuntimeSrcJar(Arrays.asList(*library.getFiles(OrderRootType.SOURCES)))
}

fun getTestJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return LibraryUtils.getJarFile(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)), PathUtil.KOTLIN_TEST_JAR)
}

fun getJsStdLibJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return LibraryUtils.getJarFile(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)), PathUtil.JS_LIB_JAR_NAME)
}

fun getJsStdLibSrcJar(library: Library): VirtualFile? {
    if (isExternalLibrary(library)) return null
    return LibraryUtils.getJarFile(Arrays.asList(*library.getFiles(OrderRootType.SOURCES)), PathUtil.JS_LIB_SRC_JAR_NAME)
}

private fun getRuntimeSrcJar(classesRoots: List<VirtualFile>): VirtualFile? {
    return LibraryUtils.getJarFile(classesRoots, PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR)
}
