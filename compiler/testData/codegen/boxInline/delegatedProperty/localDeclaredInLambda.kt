// FILE: test.kt
package test

import kotlin.reflect.KProperty

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String = p.name + this

object C {
    inline fun inlineFun() = {
        val O by "K"
        O
    }.let { it() }
}

// FILE: box.kt
import test.*

fun box(): String = C.inlineFun()
