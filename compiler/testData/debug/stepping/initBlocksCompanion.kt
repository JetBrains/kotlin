
// FILE: test.kt

class A {
    companion object {
        val s: String

        init {
            s = "OK"
        }

        val x = x()

        init { val a = 32 }

        init {
            val b = 42
        }

        init
        {
            val c = 43
        }
    }
}

fun x() = ""

fun box() {
    A.x
    A.s
}

// EXPECTATIONS JVM_IR
// test.kt:30 box
// test.kt:8 <clinit>
// test.kt:9 <clinit>
// test.kt:10 <clinit>
// test.kt:12 <clinit>
// test.kt:14 <clinit>
// test.kt:16 <clinit>
// test.kt:17 <clinit>
// test.kt:18 <clinit>
// test.kt:20 <clinit>
// test.kt:22 <clinit>
// test.kt:23 <clinit>
// test.kt:12 getX
// test.kt:12 getX
// test.kt:30 box
// test.kt:31 box
// test.kt:6 getS
// test.kt:6 getS
// test.kt:31 box
// test.kt:32 box

// EXPECTATIONS JS_IR
// test.kt:30 box
// test.kt:9 <init>
// test.kt:12 <init>
// test.kt:27 x
// test.kt:14 <init>
// test.kt:17 <init>
// test.kt:22 <init>
// test.kt:5 <init>
// test.kt:31 box
// test.kt:32 box

// EXPECTATIONS WASM
// test.kt:9 $Companion.<init> (12, 16, 12)
// test.kt:12 $Companion.<init> (16, 16, 16)
// test.kt:27 $x (10, 10, 10, 10, 12)
// test.kt:14 $Companion.<init> (23, 15)
// test.kt:17 $Companion.<init> (20, 12)
// test.kt:22 $Companion.<init> (20, 12)
// test.kt:24 $Companion.<init>
// test.kt:30 $box (6, 6)
// test.kt:31 $box
// test.kt:32 $box
