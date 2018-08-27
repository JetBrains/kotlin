/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.random_bound

import kotlin.random.*
import kotlin.test.*

@Test
fun testBoundsNextInt() {
    assertFailsWith<IllegalArgumentException>("Should fail on bound 0", { Random.nextInt(0) })
    assertFailsWith<IllegalArgumentException>("Should fail on bound -100", { Random.nextInt(-100) })
    assertFailsWith<IllegalArgumentException>("Should fail on bound ${Int.MIN_VALUE}", { Random.nextInt(Int.MIN_VALUE) })
}