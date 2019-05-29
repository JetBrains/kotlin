/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.name.FqName

private data class FacadeCacheKey(val fqName: FqName, val searchScope: GlobalSearchScope)

private data class ValueWrapper(val value: KtLightClassForFacade?) {
    companion object {
        val Null = ValueWrapper(null)
    }
}

class FacadeCache(private val project: Project) {
    private inner class FacadeCacheData {
        val cache = object : SLRUCache<FacadeCacheKey, ValueWrapper>(20, 30) {
            override fun createValue(key: FacadeCacheKey): ValueWrapper =
                KtLightClassForFacade.createForFacadeNoCache(key.fqName, key.searchScope, project)
                    ?.let { ValueWrapper(it) }
                    ?: ValueWrapper.Null
        }
    }

    private val cachedValue: CachedValue<FacadeCacheData> = CachedValuesManager.getManager(project).createCachedValue(
        { CachedValueProvider.Result.create(FacadeCacheData(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) }, false
    )

    operator fun get(qualifiedName: FqName, searchScope: GlobalSearchScope): KtLightClassForFacade? {
        synchronized(cachedValue) {
            return cachedValue.value.cache.get(FacadeCacheKey(qualifiedName, searchScope)).value
        }
    }

    companion object {
        fun getInstance(project: Project): FacadeCache {
            return ServiceManager.getService(project, FacadeCache::class.java)
        }
    }
}