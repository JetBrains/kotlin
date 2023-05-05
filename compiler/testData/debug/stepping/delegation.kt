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

// EXPECTATIONS JVM JVM_IR
// test.kt:16 box
// test.kt:13 <init>
// EXPECTATIONS JVM
// test.kt:7 <clinit>
// EXPECTATIONS JVM JVM_IR
// test.kt:16 box
// test.kt:17 box
// test.kt:10 f
// test.kt:-1 f
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:13 <init>
// test.kt:7 <init>
// test.kt:13 <init>
// test.kt:17 box
// test.kt:1 f
// test.kt:10 f
// test.kt:1 f
// test.kt:18 box
