/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze6

import kotlin.test.*
import kotlin.native.concurrent.*

@Test
fun ensureNeverFrozenNoFreezeChild(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    val nested = Nested(noFreeze)
    assertFails { nested.freeze() }

    println("OK")
}

@Test
fun ensureNeverFrozenFailsTarget(){
    val noFreeze = Hi("qwert")
    noFreeze.ensureNeverFrozen()

    assertFalse(noFreeze.isFrozen)
    assertFails { noFreeze.freeze() }
    assertFalse(noFreeze.isFrozen)
    println("OK")
}

data class Hi(val s:String)
data class Nested(val hi:Hi)