// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

// MODULE: main(lib)
// FILE: privateTopLevelValInDifferentModule.kt
import lib.*

fun box() = ref.call().s

// MODULE: lib
// FILE: lib.kt
package lib

import kotlin.reflect.jvm.isAccessible

inline class S(val s: String)

private val ok = S("OK")

val ref = ::ok.apply { isAccessible = true }
