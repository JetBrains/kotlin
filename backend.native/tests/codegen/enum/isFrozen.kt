/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.isFrozen

import kotlin.test.*
import kotlin.native.concurrent.*

enum class Zzz(val zzz: String) {
    Z1("z1"),
    Z2("z2")
}

@Test fun runTest() {
    println(Zzz.Z1.isFrozen)
}