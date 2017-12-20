// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package filter

fun main(args: Array<String>) {
    // Breakpoint!
    doubleArrayOf(1.0, 3.0, 5.0).asSequence().takeWhile { it < 2 }.forEach {}
}