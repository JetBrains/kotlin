// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.analysis.api.dumdum.StubIndex

abstract class KotlinStringStubIndexHelper<Key : NavigatablePsiElement>(private val valueClass: Class<Key>) {
    private val logger = Logger.getInstance(this.javaClass)
    abstract val indexKey: StubIndexKey<String, Key>

    operator fun get(stubIndex: StubIndex, fqName: String, project: Project, scope: GlobalSearchScope): Collection<Key> =
        buildList {
            stubIndex.processElements(indexKey, fqName, project, scope, valueClass) { key ->
                add(key)
                true
            }
        }

    fun getAllElements(
        stubIndex: StubIndex,
        key: String,
        project: Project,
        scope: GlobalSearchScope,
        filter: (Key) -> Boolean = { true },
    ): Sequence<Key> {
        val processor = CancelableCollectFilterProcessor<Key>(filter = filter)
        stubIndex.processElements(
            indexKey = indexKey,
            key = key,
            project = project,
            scope = scope,
            requiredClass = valueClass,
            processor = processor
        )
        return processor.results.asSequence() // todo move valueFilter out
    }
}

class CancelableCollectFilterProcessor<T>(
    collection: Collection<T> = mutableListOf(),
    private val filter: (T) -> Boolean,
) : CommonProcessors.CollectProcessor<T>(collection) {

    override fun process(t: T): Boolean {
        ProgressManager.checkCanceled()
        return super.process(t)
    }

    override fun accept(t: T): Boolean = filter(t)

    companion object {
        val ALWAYS_TRUE: (Any) -> Boolean = { true }
    }
}