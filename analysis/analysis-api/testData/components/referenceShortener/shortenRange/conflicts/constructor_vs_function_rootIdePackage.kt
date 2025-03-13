// FILE: main.kt
package test

import dependency.FooBar

fun test() {
    <expr>_root_ide_package_.dependency.FooBar()</expr>
}

// FILE: dependency.kt
package dependency

class FooBar(i: Int)

fun FooBar(i: String) {}
