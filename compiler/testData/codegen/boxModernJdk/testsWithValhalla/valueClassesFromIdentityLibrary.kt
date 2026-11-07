// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib.kt
package lib

class Outer {
    value class Nested(val a: Int, val b: Int)
}

value class V(val a: Int, val b: Int)

// MODULE: main(lib)
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.*

class Holder(val nested: Outer.Nested, val v: V)

fun box(): String {
    val holder = Holder(Outer.Nested(1, 2), V(3, 4))
    if (holder.nested.javaClass.isValue != false || holder.v.javaClass.isValue != false) return "Fail"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors : Llib/Outer\$Nested;, Llib/V;
// 2 public final static synchronized INNERCLASS lib/Outer\$Nested lib/Outer Nested
