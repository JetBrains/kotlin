// FILE: test.kt

annotation class Anno

class C {
    @Anno
    fun f(p: String) {
    }
}

fun box() {
    val c = C()

    c.f("")

    @Anno
    fun local() {
    }

    local()
}

// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:5 <init>
// test.kt:12 box
// test.kt:14 box
// test.kt:8 f
// test.kt:20 box
// test.kt:18 box$local
// test.kt:21 box

// EXPECTATIONS NATIVE
// test.kt:12 box
// test.kt:5 <init>
// test.kt:9 <init>
// test.kt:12 box
// test.kt:14 box
// test.kt:7 f
// test.kt:8 f
// test.kt:20 box
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:5 <init>
// test.kt:14 box
// test.kt:8 f
// test.kt:20 box
// test.kt:18 box$local
// test.kt:21 box

// EXPECTATIONS WASM
// test.kt:12 $box (12)
// test.kt:9 $C.<init> (1)
// test.kt:14 $box (4, 8, 6)
// test.kt:8 $C.f (5)
// test.kt:20 $box (4)
// test.kt:18 $box$local (5)
// test.kt:21 $box (1)
