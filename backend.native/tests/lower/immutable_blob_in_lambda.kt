/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package lower.immutable_blob_in_lambda

import kotlin.test.*

@Test
fun runTest() = run {
    val golden = immutableBlobOf(123)
    println(golden[0])
}