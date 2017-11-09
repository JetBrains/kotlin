// FILE: 1.kt

package test

inline fun inlineFun(vararg constraints: String, receiver: String = "O", init: String.() -> String): String {
    return receiver.init()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return inlineFun {
        this + "K"
    }
}

