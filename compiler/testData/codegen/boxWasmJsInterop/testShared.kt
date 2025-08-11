// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS

// FILE: externals.kt

class MyClass(val a: Int, val b: Int)
var a = MyClass(1, 2)

fun box(): String {
    return if (a.a == 1) "OK" else "Fail"
}