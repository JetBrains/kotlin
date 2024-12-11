// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.CommonProcessors
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.analysis.api.dumdum.KeyType
import org.jetbrains.kotlin.analysis.api.dumdum.StubIndex
import org.jetbrains.kotlin.analysis.api.dumdum.StubIndexExtension
import java.io.DataInput
import java.io.DataOutput

abstract class KotlinStringStubIndexHelper<Key : NavigatablePsiElement>(
    private val valueClass: Class<Key>,
) : StubIndexExtension<String, Key> {

    companion object {
        object StringKeyDescriptor : KeyDescriptor<String> {
            override fun getHashCode(value: String?): Int =
                value.hashCode()

            override fun isEqual(val1: String?, val2: String?): Boolean =
                val1 == val2

            override fun save(out: DataOutput, value: String?) {
                out.writeUTF(value!!)
            }

            override fun read(`in`: DataInput): String =
                `in`.readUTF()
        }
    }

    override val version: Int = 1

    override val keyDescriptor: KeyDescriptor<String> = StringKeyDescriptor

    private val logger = Logger.getInstance(this.javaClass)

    override val key: StubIndexKey<String, Key>
        get() = indexKey

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