// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS, JS_IR
// WITH_REFLECT
// FILE: 1.kt

package foo

import kotlin.reflect.full.primaryConstructor

inline fun <reified T : Any> f(): String {
    var result = ""
    for (p in T::class.primaryConstructor!!.parameters.sortedBy { it.index }) {
        result += p.name
    }
    return result
}

// FILE: 2.kt
import foo.*

class A(val O: Int, val K: Int)

fun box(): String {
    return f<A>()
}
