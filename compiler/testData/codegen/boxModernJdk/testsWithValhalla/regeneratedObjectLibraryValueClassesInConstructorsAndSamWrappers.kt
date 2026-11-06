// LANGUAGE: +FullValueClasses
// SAM_CONVERSIONS: CLASS
// CHECK_BYTECODE_TEXT
// MODULE: lib
// FILE: lib/JavaVBox.java
package lib;

public interface JavaVBox {
    V get();
}

// FILE: lib.kt
package lib

value class V(val a: Int, val b: Int)

abstract class Base(val v: V)

inline fun make(v: V): Base = object : Base(v) {}

inline fun makeSamBox(noinline supplier: () -> V): JavaVBox = JavaVBox(supplier)

// MODULE: main(lib)
// VALHALLA_VALUE_CLASSES
// FILE: main.kt
import lib.*

// The library is compiled without Valhalla value classes, unlike this module.
fun box(): String {
    if (make(V(1, 2)).v != V(1, 2)) return "Fail: make"
    if (makeSamBox { V(3, 4) }.get() != V(3, 4)) return "Fail: makeSamBox"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : Llib/V;
