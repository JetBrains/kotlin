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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.containers.ContainerUtil
import java.io.IOException

class ClsJavaStubByVirtualFileCache {
    private class CachedJavaStub(val modificationStamp: Long, val javaFileStub: PsiJavaFileStubImpl)

    private val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<VirtualFile, CachedJavaStub>()

    fun get(classFile: VirtualFile): PsiJavaFileStubImpl? {
        val cached = cache.get(classFile)
        val fileModificationStamp = classFile.modificationStamp
        if (cached != null && cached.modificationStamp == fileModificationStamp) {
            return cached.javaFileStub
        }
        val stub = createStub(classFile) as PsiJavaFileStubImpl? ?: return null
        cache.put(classFile, CachedJavaStub(fileModificationStamp, stub))
        return stub
    }

    private fun createStub(file: VirtualFile): PsiJavaFileStub? {
        if (file.fileType !== JavaClassFileType.INSTANCE) return null

        try {
            return ClsFileImpl.buildFileStub(file, file.contentsToByteArray(false))
        } catch (e: ClsFormatException) {
            LOG.error("Failed to build java cls class for " + file.canonicalPath!!, e)
        } catch (e: IOException) {
            LOG.error("Failed to build java cls class for " + file.canonicalPath!!, e)
        }

        return null
    }

    companion object {
        private val LOG = Logger.getInstance(ClsJavaStubByVirtualFileCache::class.java)

        fun getInstance(project: Project): ClsJavaStubByVirtualFileCache {
            return ServiceManager.getService(project, ClsJavaStubByVirtualFileCache::class.java)
        }
    }
}