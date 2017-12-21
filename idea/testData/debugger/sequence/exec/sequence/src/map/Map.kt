// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package map

fun main(args: Array<String>) {
  // Breakpoint!
  doubleArrayOf(1.0, 2.0).asSequence().map { it * it }.contains(3.0)
}