// IGNORE_BACKEND: WASM
// FILE: test.kt

interface I {
    fun f()
}

object O : I {
    override fun f() {
        Unit
    }
}

class C : I by O

fun box() {
    val c = C()
    c.f()
}

// EXPECTATIONS JVM_IR
// test.kt:17 box
// test.kt:14 <init>
// test.kt:17 box
// test.kt:18 box
// test.kt:11 f
// test.kt:-1 f
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:17 box
// test.kt:14 <init>
// test.kt:8 <init>
// test.kt:14 <init>
// test.kt:18 box
// test.kt:1 f
// test.kt:11 f
// test.kt:1 f
// test.kt:19 box
