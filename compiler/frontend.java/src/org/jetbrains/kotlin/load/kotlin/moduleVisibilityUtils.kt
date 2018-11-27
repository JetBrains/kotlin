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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeAliasDescriptor
import java.io.File

interface ModuleVisibilityManager {
    val chunk: Collection<Module>
    val friendPaths: Collection<String>
    fun addModule(module: Module)
    fun addFriendPath(path: String)
    val enabled
        get() = true

    object SERVICE {
        @JvmStatic fun getInstance(project: Project): ModuleVisibilityManager =
                ServiceManager.getService(project, ModuleVisibilityManager::class.java)
    }
}

fun isContainedByCompiledPartOfOurModule(descriptor: DeclarationDescriptor, outDirectory: File?): Boolean {
    if (outDirectory == null) return false

    val packageFragment = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java, false)
    if (packageFragment !is LazyJavaPackageFragment) return false

    val source = getSourceElement(descriptor)

    val binaryClass = when (source) {
        is KotlinJvmBinarySourceElement ->
            source.binaryClass
        is KotlinJvmBinaryPackageSourceElement ->
            if (descriptor is DeserializedMemberDescriptor) {
                source.getContainingBinaryClass(descriptor) ?: source.getRepresentativeBinaryClass()
            }
            else {
                source.getRepresentativeBinaryClass()
            }
        else ->
            null
    }

    if (binaryClass is VirtualFileKotlinClass) {
        val file = binaryClass.file
        val ioFile = when (file.fileSystem.protocol) {
            StandardFileSystems.FILE_PROTOCOL -> VfsUtilCore.virtualToIoFile(file)
            StandardFileSystems.JAR_PROTOCOL -> VfsUtilCore.getVirtualFileForJar(file)?.let(VfsUtilCore::virtualToIoFile)
            else -> null
        }
        return ioFile != null && ioFile.toPath().startsWith(outDirectory.toPath())
    }

    return false
}

fun getSourceElement(descriptor: DeclarationDescriptor): SourceElement =
        when {
            descriptor is CallableMemberDescriptor && descriptor.source === SourceElement.NO_SOURCE ->
                getSourceElement(descriptor.containingDeclaration)
            descriptor is DeserializedTypeAliasDescriptor ->
                getSourceElement(descriptor.containingDeclaration)
            else ->
                descriptor.toSourceElement
        }

val DeclarationDescriptor.toSourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE
