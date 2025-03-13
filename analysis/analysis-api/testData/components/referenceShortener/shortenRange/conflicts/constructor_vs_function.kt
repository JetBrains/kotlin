// FILE: main.kt
package test

import dependency.FooBar

fun test() {
    <expr>dependency.FooBar()</expr>
}

// FILE: dependency.kt
package dependency

class FooBar(i: Int)

fun FooBar(i: String) {}
