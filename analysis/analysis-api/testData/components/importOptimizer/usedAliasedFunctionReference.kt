// FILE: main.kt
package test

import dependency.foo as aliasedFoo

fun usage() {
    val ref = ::aliasedFoo
}

// FILE: dependency.kt
package dependency

fun foo() {}