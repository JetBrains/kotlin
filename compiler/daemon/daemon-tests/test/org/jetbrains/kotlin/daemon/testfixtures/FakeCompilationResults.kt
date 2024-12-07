/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.testfixtures

import org.jetbrains.kotlin.daemon.common.CompilationResults
import java.io.Serializable

class FakeCompilationResults : CompilationResults {

    val results: List<Serializable>
        get() = resultList.toList()

    private val resultList = mutableListOf<Serializable>()

    override fun add(compilationResultCategory: Int, value: Serializable) {
        resultList.add(value)
    }
}
