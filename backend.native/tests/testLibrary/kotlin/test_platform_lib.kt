/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.konan.platform

fun produceMessage() {
    println("""This is a side effect of a test library linked into the binary.
You should not be seeing this.
""")
}

val x: Unit = produceMessage()
