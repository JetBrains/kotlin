// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun test(cond: Boolean, crossinline cif: () -> String): String {
    return if (cond) {
        { cif() }()
    }
    else {
        cif()
    }
}
// FILE: 2.kt

import test.*

fun box(): String {
    val s = "OK"
    return test(true) {
        {
            s
        }()
    }
}

