// FILE: main.kt
package test

import dependency.FooBar

fun test() {
    <expr>dependency.FooBar()</expr>
}

// FILE: dependency.kt
package dependency

class RegularClass(i: Int)

typealias FooBar = RegularClass

fun FooBar(i: String) {}

// TODO This test is not correct, see KTIJ-33500