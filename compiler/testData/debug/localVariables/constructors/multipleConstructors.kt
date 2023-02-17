// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt
open class Base(i: Int)

class Derived(): Base(1) {
    constructor(p: Int): this() {
        val a = 2
    }

    constructor(p1: Int, p2: Int): this()
}

fun box() {
    Derived(3)
    Derived(4, 5)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box:
// test.kt:7 <init>: p:int=3:int
// test.kt:6 <init>:
// test.kt:4 <init>: i:int=1:int
// test.kt:6 <init>:
// test.kt:8 <init>: p:int=3:int
// EXPECTATIONS JVM_IR
// test.kt:9 <init>: p:int=3:int, a:int=2:int
// EXPECTATIONS JVM JVM_IR
// test.kt:15 box:
// test.kt:16 box:
// test.kt:11 <init>: p1:int=4:int, p2:int=5:int
// test.kt:6 <init>:
// test.kt:4 <init>: i:int=1:int
// test.kt:6 <init>:
// test.kt:11 <init>: p1:int=4:int, p2:int=5:int
// test.kt:16 box:
// test.kt:17 box:

// EXPECTATIONS JS_IR
// test.kt:15 box:
// test.kt:7 Derived_init_$Init$: p=3:number
// test.kt:6 <init>:
// test.kt:4 <init>: i=1:number
// test.kt:6 <init>:
// test.kt:8 Derived_init_$Init$: p=3:number
// test.kt:16 box:
// test.kt:11 Derived_init_$Init$: p1=4:number, p2=5:number
// test.kt:6 <init>:
// test.kt:4 <init>: i=1:number
// test.kt:6 <init>:
// test.kt:17 box:
