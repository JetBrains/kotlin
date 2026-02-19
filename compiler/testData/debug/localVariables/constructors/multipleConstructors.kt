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

// EXPECTATIONS JVM_IR
// test.kt:13 box:
// test.kt:5 <init>: p:int=3:int
// test.kt:4 <init>:
// test.kt:2 <init>: i:int=1:int
// test.kt:4 <init>:
// test.kt:6 <init>: p:int=3:int
// test.kt:7 <init>: p:int=3:int, a:int=2:int
// test.kt:13 box:
// test.kt:14 box:
// test.kt:9 <init>: p1:int=4:int, p2:int=5:int
// test.kt:4 <init>:
// test.kt:2 <init>: i:int=1:int
// test.kt:4 <init>:
// test.kt:9 <init>: p1:int=4:int, p2:int=5:int
// test.kt:14 box:
// test.kt:15 box:

// EXPECTATIONS JS_IR
// test.kt:13 box:
// test.kt:5 Derived_init_$Init$: p=3:number
// test.kt:4 <init>:
// test.kt:2 <init>: i=1:number
// test.kt:4 <init>:
// test.kt:6 Derived_init_$Init$: p=3:number
// test.kt:14 box:
// test.kt:9 Derived_init_$Init$: p1=4:number, p2=5:number
// test.kt:4 <init>:
// test.kt:2 <init>: i=1:number
// test.kt:4 <init>:
// test.kt:15 box:
