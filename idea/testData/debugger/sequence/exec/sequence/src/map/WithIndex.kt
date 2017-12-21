// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package map

fun main(args: Array<String>) {
  // Breakpoint!
  intArrayOf(1, 2, 3).asSequence().withIndex().forEach {}
}