/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.kotlin.idea.AbstractResolveElementCacheTest

class FirResolveStateTest : AbstractResolveElementCacheTest() {
    fun testSimpleStateCaching() {
        doTest {
            val firstStatement = statements[0]
            val secondStatement = statements[1]
            assertSame(firstStatement.firResolveState(), secondStatement.firResolveState())
        }
    }
}