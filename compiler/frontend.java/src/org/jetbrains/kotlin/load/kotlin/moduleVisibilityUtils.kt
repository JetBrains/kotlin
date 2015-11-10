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
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import java.io.File

interface ModuleVisibilityManager {
    val chunk: Collection<Module>
    val friendPaths: Collection<String>
    fun addModule(module: Module)
    fun addFriendPath(path: String)

    public object SERVICE {
        @JvmStatic
        public fun getInstance(project: Project): ModuleVisibilityManager =
                ServiceManager.getService(project, ModuleVisibilityManager::class.java)
    }
}

public fun isContainedByCompiledPartOfOurModule(descriptor: DeclarationDescriptor, outDirectory: File?): Boolean {
    val packageFragment = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java, false)

    if (packageFragment is IncrementalPackageFragmentProvider.IncrementalPackageFragment) return true

    if (outDirectory == null || packageFragment !is LazyJavaPackageFragment) return false

    val source = getSourceElement(descriptor)

    val binaryClass = when (source) {
        is KotlinJvmBinarySourceElement ->
            source.binaryClass
        is KotlinJvmBinaryPackageSourceElement ->
            if (descriptor is DeserializedCallableMemberDescriptor) {
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
        if (file.fileSystem.protocol == StandardFileSystems.FILE_PROTOCOL) {
            val ioFile = VfsUtilCore.virtualToIoFile(file)
            return ioFile.absolutePath.startsWith(outDirectory.absolutePath + File.separator);
        }
    }

    return false;
}

fun getSourceElement(descriptor: DeclarationDescriptor): SourceElement =
        if (descriptor is CallableMemberDescriptor && descriptor.source === SourceElement.NO_SOURCE) {
            descriptor.containingDeclaration.toSourceElement
        }
        else {
            descriptor.toSourceElement
        }

private val DeclarationDescriptor.toSourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE