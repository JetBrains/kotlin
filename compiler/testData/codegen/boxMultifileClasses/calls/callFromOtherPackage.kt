// FILE: 1.kt

import a.foo

fun box(): String = foo()

// FILE: 2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

fun foo(): String = "OK"
