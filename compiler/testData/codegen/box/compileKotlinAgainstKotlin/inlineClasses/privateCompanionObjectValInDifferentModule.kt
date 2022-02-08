// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

// MODULE: lib
// FILE: A.kt
package a

import kotlin.reflect.jvm.isAccessible

inline class S(val s: String)

class Host {
    companion object {
        private val ok = S("OK")
        val ref = ::ok.apply { isAccessible = true }
    }
}

// MODULE: main(lib)
// FILE: B.kt
import a.*

fun box() = Host.ref.call().s
