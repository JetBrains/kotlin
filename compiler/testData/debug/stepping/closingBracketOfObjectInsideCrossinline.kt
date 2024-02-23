// See KT-64727

// WITH_STDLIB
// FILE: test.kt

inline fun <reified T : Any> foo(crossinline function: () -> T) {


    object {
        fun bar() {
            function()
        }

        init {
            bar()
        }
    }
}

fun box() {
    var result = "Fail"
    foo {
        object {
            init {
                result = "OK"
            } // Should be hit
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:22 box
// test.kt:9 box
// test.kt:9 <init>
// test.kt:14 <init>
// test.kt:15 <init>
// test.kt:11 bar
// test.kt:23 bar
// test.kt:23 <init>
// test.kt:24 <init>
// test.kt:25 <init>
// test.kt:26 <init>
// test.kt:23 <init>
// test.kt:27 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:16 <init>
// test.kt:9 <init>
// test.kt:9 box
// test.kt:18 box
// test.kt:29 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:9 box
// test.kt:15 <init>
// test.kt:23 bar
// test.kt:25 <init>
// test.kt:23 <init>
// test.kt:12 bar
// test.kt:9 <init>
// test.kt:29 box

// EXPECTATIONS WASM
// test.kt:21 $box (17, 4)
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
// ClosureBoxes.kt:8 $kotlin.wasm.internal.ClosureBoxAny.<init> (29, 44)
// test.kt:22 $box
// test.kt:9 $box (4, 4)
// test.kt:15 $<no name provided>.<init>
// test.kt:11 $<no name provided>.bar
// kotlin-package.kt:9 $<no name provided>.bar (37, 37)
// test.kt:25 $<no name provided>.<init> (16, 25, 25, 25, 25, 16)
// test.kt:27 $<no name provided>.<init>
// kotlin-package.kt:10 $<no name provided>.bar
// test.kt:12 $<no name provided>.bar
// test.kt:17 $<no name provided>.<init>
// test.kt:29 $box
