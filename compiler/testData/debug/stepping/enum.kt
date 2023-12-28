// IGNORE_BACKEND_K2: WASM
// FILE: test.kt

enum class E() {
    A,
    B;

    fun foo() = {
        prop
    }

    val prop = 22
}

enum class E2(val y : Int) {
    C(1),
    D(
        2
    )
}

fun box() {
    E.A.foo()
    E2.C;
}

// EXPECTATIONS JVM_IR
// test.kt:23 box
// test.kt:5 <clinit>
// test.kt:6 <clinit>
// test.kt:8 foo
// test.kt:10 foo
// test.kt:23 box
// test.kt:24 box
// test.kt:16 <clinit>
// test.kt:17 <clinit>
// test.kt:18 <clinit>
// test.kt:17 <clinit>
// test.kt:25 box

// EXPECTATIONS JS_IR
// test.kt:23 box
// test.kt:12 <init>
// test.kt:4 <init>
// test.kt:12 <init>
// test.kt:4 <init>
// test.kt:23 box
// test.kt:10 foo
// test.kt:8 E$foo$lambda
// test.kt:16 E2_initEntries
// test.kt:15 <init>
// test.kt:15 <init>
// test.kt:18 E2_initEntries
// test.kt:15 <init>
// test.kt:15 <init>
// test.kt:25 box

// EXPECTATIONS WASM
// test.kt:1 $box
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5, 5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4, 11, 4, 11, 4)
// Enum.kt:9 $kotlin.Enum.<init> (4, 4, 4, 4)
// Enum.kt:11 $kotlin.Enum.<init> (4, 4, 4, 4)
// Enum.kt:27 $kotlin.Enum.<init> (1, 1, 1, 1)
// test.kt:12 $E.<init> (15, 15)
// test.kt:13 $E.<init> (1, 1)
// test.kt:23 $box
// test.kt:8 $E.foo (16, 16)
// test.kt:10 $E.foo
// test.kt:16 $E2_initEntries
// test.kt:15 $E2.<init> (14, 14)
// test.kt:20 $E2.<init> (1, 1)
// test.kt:18 $E2_initEntries
// test.kt:25 $box
