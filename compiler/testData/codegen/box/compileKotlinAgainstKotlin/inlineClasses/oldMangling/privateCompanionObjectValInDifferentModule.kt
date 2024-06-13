// TARGET_BACKEND: JVM
// LANGUAGE: +InlineClasses
// WITH_REFLECT
// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
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
