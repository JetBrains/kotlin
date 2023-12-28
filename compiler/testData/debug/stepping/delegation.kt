
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

// EXPECTATIONS WASM
// test.kt:17 $box (12, 12)
// test.kt:14 $C.<init> (15, 16)
// test.kt:18 $box (4, 6)
// test.kt:1 $C.f
// test.kt:11 $O.f
// test.kt:19 $box
