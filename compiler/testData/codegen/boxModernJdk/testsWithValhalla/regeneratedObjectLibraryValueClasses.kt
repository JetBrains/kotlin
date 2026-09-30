// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
package lib

value class V(val a: Int, val b: Int)

inline fun capture(v: V): Any = object {
    val copy = v

    override fun toString() = "${copy.a}${v.b}"
}

// MODULE: main(lib)
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.*

// The library is compiled without Valhalla value classes, unlike this module.
fun box(): String = if (capture(V(1, 2)).toString() == "12") "OK" else "Fail"

// 0 LoadableDescriptors
