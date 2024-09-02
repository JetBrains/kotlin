
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
// test.kt:12 $E.<init> (15, 15, 15, 15, 15, 15)
// test.kt:4 $E.<init> (14, 14)
// test.kt:23 $box
// test.kt:8 $E.foo (16, 16, 16)
// test.kt:10 $E.foo
// test.kt:16 $E2_initEntries
// test.kt:15 $E2.<init> (14, 26, 14, 26)
// test.kt:18 $E2_initEntries
// test.kt:25 $box
