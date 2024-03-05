// FIR_IDENTICAL
// ISSUE: KT-65789
// FIR_DUMP

// FILE: Foo.kt
package bar

fun Foo(string: String?) {}

class Foo(val str: String)

val foo = Foo("1")
val barFoo = bar.Foo("2")

// FILE: another.kt
package foo

import bar.Foo

val foo = Foo("3")
