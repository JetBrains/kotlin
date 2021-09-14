// FILE: main.kt
package test

import dependency.foo

fun usage() {
    foo()
}

// FILE: dependency.kt
package dependency

fun foo() {}