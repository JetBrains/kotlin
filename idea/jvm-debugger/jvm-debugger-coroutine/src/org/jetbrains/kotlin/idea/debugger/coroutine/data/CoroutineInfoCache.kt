/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

class CoroutineInfoCache(
    val cache: MutableList<CoroutineInfoData> = mutableListOf(), var state: CacheState = CacheState.INIT
) {
    fun ok(infoList: List<CoroutineInfoData>) {
        cache.clear()
        cache.addAll(infoList)
        state = CacheState.OK
    }

    fun fail() {
        cache.clear()
        state = CacheState.FAIL
    }

    fun isOk(): Boolean {
        return state == CacheState.OK
    }
}

enum class CacheState {
    OK, FAIL, INIT
}