
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
// test.kt:1 $box
// test.kt:9 $Companion.<init> (12, 16, 16, 16, 16, 12)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4)
// test.kt:12 $Companion.<init>
// test.kt:27 $x (10, 10, 10, 10, 12)
// test.kt:14 $Companion.<init> (23, 15)
// test.kt:17 $Companion.<init> (20, 12)
// test.kt:22 $Companion.<init> (20, 12)
// test.kt:24 $Companion.<init>
// test.kt:30 $box
// test.kt:31 $box
// test.kt:32 $box
