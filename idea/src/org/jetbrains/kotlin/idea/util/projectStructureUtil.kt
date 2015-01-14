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

package org.jetbrains.kotlin.idea.util.projectStructure

import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import java.io.File
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.roots.OrderRootType

public fun Module.findLibrary(predicate: (Library) -> Boolean): Library? = OrderEnumerator.orderEntries(this).findLibrary(predicate)

public fun OrderEnumerator.findLibrary(predicate: (Library) -> Boolean): Library? {
    var lib: Library? = null
    forEachLibrary { library ->
        if (predicate(library!!)) {
            lib = library
            false
        }
        else {
            true
        }
    }

    return lib
}

public fun Module.getModuleDir(): String = File(getModuleFilePath()).getParent()!!.replace(File.separatorChar, '/')

public fun Library.ModifiableModel.replaceFileRoot(oldFile: File, newFile: File) {
    val oldRoot = VfsUtil.getUrlForLibraryRoot(oldFile)
    val newRoot = VfsUtil.getUrlForLibraryRoot(newFile)

    fun replaceInRootType(rootType: OrderRootType) {
        for (url in getUrls(rootType)) {
            if (oldRoot == url) {
                removeRoot(url, rootType)
                addRoot(newRoot, rootType)
            }
        }
    }

    replaceInRootType(OrderRootType.CLASSES)
    replaceInRootType(OrderRootType.SOURCES)
}




