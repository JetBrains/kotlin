/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.generators

import java.io.File

interface TreeGenerator {
    fun generateBaseTree(text: String, file: File)
    fun generateFir(text: String, file: File, stubMode: Boolean)
    fun setUp()
    fun tearDown()
}