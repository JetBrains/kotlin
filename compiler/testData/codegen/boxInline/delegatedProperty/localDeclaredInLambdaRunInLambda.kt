// ISSUE: KT-85605

// FILE: test.kt
package test

import kotlin.reflect.KProperty

inline operator fun String.getValue(t: Any?, p: KProperty<*>): String = p.name + this

object C {
    inline fun inlineFun() = run {
        val O by "K"
        O
    }
}

// FILE: box.kt
import test.*

fun box() = run { C.inlineFun() }
