/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import org.jetbrains.kotlin.idea.completion.ml.KotlinMLRankingProvider
import org.junit.Test

class KotlinModelMetadataTest {
    @Test
    fun testMetadataConsistent() = KotlinMLRankingProvider().assertModelMetadataConsistent()
}