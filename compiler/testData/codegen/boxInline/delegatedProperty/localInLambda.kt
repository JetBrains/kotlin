// FILE: 1.kt
package test

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String = "OK"
}

inline fun test(): String {
    val b by Delegate()

    return run {
        b
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    return test()
}
