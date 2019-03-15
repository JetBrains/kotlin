// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class W(val value: Any)

inline fun W.safe(crossinline body : Any.() -> Unit) {
    {
        this.value?.body()
    }()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    W("OK").safe {
        result = this as String
    }

    return result
}
