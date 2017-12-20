// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package filter

fun main(args: Array<String>) {
    // Breakpoint!
    booleanArrayOf(true, false, false).asSequence().filterNot { it }.lastIndexOf(true)
}