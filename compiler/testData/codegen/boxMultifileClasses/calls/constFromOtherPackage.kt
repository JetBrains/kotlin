// FILE: 1.kt

import a.OK

fun box(): String = OK

// FILE: 2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

const val OK: String = "OK"
