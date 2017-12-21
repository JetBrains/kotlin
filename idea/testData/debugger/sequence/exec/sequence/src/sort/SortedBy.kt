// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package sort

fun main(args: Array<String>) {
  // Breakpoint!
  arrayOf(Person("Bob", 42), Person("Alice", 27)).asSequence().sortedBy { it.age }.count()
}

internal data class Person(val name: String, val age: Int)