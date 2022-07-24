/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.dumpCollection

import org.jetbrains.kotlin.name.FqName
import java.io.File

class ApiListenersDataMap(storageFile: File) : BasicStringMap<Collection<FqName>>(storageFile, FqNameCollectionExternalizer) {

    operator fun get(listenerId: String): Collection<FqName> {
        return storage[listenerId] ?: setOf()
    }

    operator fun set(listenerId: String, subscribedFqNames: List<FqName>) {
        val oldValue = storage[listenerId]
        if (oldValue == subscribedFqNames) return
        storage[listenerId] = subscribedFqNames
    }

    fun remove(listenerId: String) {
        storage.remove(listenerId)
    }

    val keys: Collection<String>
        get() = storage.keys


    override fun dumpValue(value: Collection<FqName>): String = value.map { it.toString() }.dumpCollection()
}
