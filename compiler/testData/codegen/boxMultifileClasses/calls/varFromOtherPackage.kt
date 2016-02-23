// FILE: 1.kt

import a.OK

fun box(): String {
    OK = "OK"
    return OK
}

// FILE: 2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

var OK: String = "Hmmm?"
