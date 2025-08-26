// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// DISABLE_WASM_EXCEPTION_HANDLING

// FILE: shared.kt

class MyClass(val a: Int, val b: Int) : Function2<Int, Int, Int> {
    override fun invoke(p1: Int, p2: Int): Int = p1 - a + p2 * b
}

var a = MyClass(1, 2)

fun box(): String {
    return if (a.a == 1 && MyClass(1, 2).invoke(3, 4) == 10) "OK" else "Fail"
}