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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.testFramework.BinaryLightVirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.LIBRARY_NAME_PREFIX
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import kotlin.platform.platformStatic

fun getKotlinJavascriptLibrary(descriptor: DeclarationDescriptor, project: Project): Library? {
    val containingPackageFragment = DescriptorUtils.getParentOfType<PackageFragmentDescriptor>(descriptor, javaClass<PackageFragmentDescriptor>())
    if (containingPackageFragment == null) return null

    val module = DescriptorUtils.getContainingModule(descriptor)
    assert(module is ModuleDescriptorImpl)

    var moduleName = getModuleName(module)

    return getKotlinJavascriptLibrary(moduleName, project)
}

fun getKotlinJavascriptLibrary(libraryName: String, project: Project): Library? {
    var library: Library? = null
    val jsModules = ModuleManager.getInstance(project).getModules().filter { (ProjectStructureUtil.isJsKotlinModule(it)) }
    jsModules.forEach { module ->
        ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary {
            if (it.getName() == libraryName) { library = it; false } else true
        }
    }

    return library
}

fun getKotlinJavascriptLibrary(file: VirtualFile, project: Project): Library? {
    val module = file.getUserData(JsMetaFileVirtualFileHolder.MODULE_DESCRIPTOR_KEY)
    if (module == null) return null

    var moduleName = getModuleName(module)

    return getKotlinJavascriptLibrary(moduleName, project)
}

private fun getModuleName(module: ModuleDescriptor): String {
    var moduleName = module.getName().asString()
    moduleName = moduleName.substring(1, moduleName.length() - 1)
    if (moduleName.startsWith(LIBRARY_NAME_PREFIX)) {
        moduleName = moduleName.substring(LIBRARY_NAME_PREFIX.length())
    }

    return moduleName
}


class JsMetaFileVirtualFileHolder private(val myProject: Project) {
    default object {
        public val MODULE_DESCRIPTOR_KEY: Key<ModuleDescriptorImpl> = Key.create("MODULE_DESCRIPTOR")
        public val PACKAGE_FQNAME_KEY: Key<FqName> = Key.create("PACKAGE_FQNAME_KEY")
        private val JS_META_FILE_HOLDER_KEY: Key<JsMetaFileVirtualFileHolder> = Key.create("JS_META_FILE_HOLDER_KEY")

        platformStatic
        public fun getInstance(project: Project): JsMetaFileVirtualFileHolder {
            var holder = project.getUserData(JS_META_FILE_HOLDER_KEY)

            if (holder == null) {
                holder = JsMetaFileVirtualFileHolder(project)
                project.putUserData(JS_META_FILE_HOLDER_KEY, holder)
            }

            return holder
        }
    }

    private val cachedFileMap = CachedValuesManager.getManager(myProject).createCachedValue( {
        CachedValueProvider.Result(hashMapOf<String, VirtualFile>(), ProjectRootModificationTracker.getInstance(myProject))
    }, false)

    fun getFile(descriptor: DeclarationDescriptor): VirtualFile? {
        val containingPackageFragment = DescriptorUtils.getParentOfType<PackageFragmentDescriptor>(descriptor, javaClass<PackageFragmentDescriptor>())
        if (containingPackageFragment == null) return null

        val packageName = DescriptorUtils.getFqName(containingPackageFragment).asString()

        val module = DescriptorUtils.getContainingModule(descriptor)
        assert(module is ModuleDescriptorImpl)

        var moduleName = module.getName().asString()
        moduleName = moduleName.substring(1, moduleName.length() - 1)
        if (moduleName.startsWith("library ")) {
            moduleName = moduleName.substring(8)
        }

        val fileName = "${moduleName}/${packageName}.meta"
        val fileMap = cachedFileMap.getValue()

        var result = fileMap[fileName]

        if (result == null) {
            result = JsMetaFileBinaryLightVirtualFile(fileName)
            fileMap[fileName] = result!!
        }

        result?.putUserData(MODULE_DESCRIPTOR_KEY, module as ModuleDescriptorImpl)
        result?.putUserData(PACKAGE_FQNAME_KEY, FqName(packageName))

        return result
    }

    private inner class JsMetaFileBinaryLightVirtualFile(name: String) : BinaryLightVirtualFile(name) {
        override fun getFileType(): FileType = JavaClassFileType.INSTANCE
    }
}

