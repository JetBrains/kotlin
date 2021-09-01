// FILE: main.kt
package test

import dependency.foo
import dependency.bar

fun usage() {}

// FILE: dependency.kt
package dependency

fun foo() {}

fun bar() {}