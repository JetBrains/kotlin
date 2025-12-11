// FILE: test.kt

annotation class Anno

class C
    @Anno
    constructor()
{
    @Anno
    constructor(p: String = "") : this()
}

fun box() {
    C()

    C("")
}

// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:5 <init>
// test.kt:7 <init>
// test.kt:14 box
// test.kt:16 box
// test.kt:10 <init>
// test.kt:5 <init>
// test.kt:7 <init>
// test.kt:10 <init>
// test.kt:16 box
// test.kt:17 box

// EXPECTATIONS NATIVE
// test.kt:14 box
// test.kt:7 <init>
// test.kt:14 box
// test.kt:16 box
// test.kt:10 <init>
// test.kt:7 <init>
// test.kt:10 <init>
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// test.kt:7 <init>
// test.kt:16 box
// test.kt:10 C_init_$Init$
// test.kt:10 C_init_$Init$
// test.kt:7 <init>
// test.kt:17 box

// EXPECTATIONS WASM
// test.kt:14 $box (4)
// test.kt:7 $C.<init> (17)
// test.kt:14 $box (4)
// test.kt:16 $box (4, 6, 4)
// test.kt:10 $C.<init> (34)
// test.kt:7 $C.<init> (17)
// test.kt:10 $C.<init> (34, 40)
// test.kt:16 $box (4)
// test.kt:17 $box (1)
