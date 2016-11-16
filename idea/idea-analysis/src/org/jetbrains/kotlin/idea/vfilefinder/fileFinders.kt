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

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

private fun checkScopeForFinder(scope: GlobalSearchScope, logger: Logger) {
    if (scope != GlobalSearchScope.EMPTY_SCOPE && scope.project == null) {
        logger.warn("Scope with null project " + scope)
    }
}

private fun findVirtualFileWithHeader(classId: ClassId, key: ID<FqName, Void>, scope: GlobalSearchScope, logger: Logger): VirtualFile? {
    val files = FileBasedIndex.getInstance().getContainingFiles<FqName, Void>(key, classId.asSingleFqName(), scope)
    if (files.isEmpty()) return null

    if (files.size > 1) {
        logger.warn("There are " + files.size + " classes with same fqName: " + classId + " found.")
    }
    return files.iterator().next()
}

class JsIDEVirtualFileFinder(private val scope: GlobalSearchScope) : JsVirtualFileFinder {

    init { checkScopeForFinder(scope, LOG) }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
            findVirtualFileWithHeader(classId, KotlinJavaScriptMetaFileIndex.KEY, scope, LOG)

    companion object {
        private val LOG = Logger.getInstance(JsIDEVirtualFileFinder::class.java)
    }
}

class JvmIDEVirtualFileFinder(private val scope: GlobalSearchScope) : VirtualFileKotlinClassFinder(), JvmVirtualFileFinder {
    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO: load built-ins metadata from scope
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null

    init { checkScopeForFinder(scope, LOG) }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
            findVirtualFileWithHeader(classId, KotlinClassFileIndex.KEY, scope, LOG)

    companion object {
        private val LOG = Logger.getInstance(JvmIDEVirtualFileFinder::class.java)
    }
}
