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

// EXPECTATIONS WASM
// test.kt:13 $box: (4, 12, 4)
// test.kt:5 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p:i32=3:i32, $a:i32=0:i32 (25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25)
// test.kt:4 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived) (22, 22, 22, 22, 22, 0)
// test.kt:2 $Base.<init>: $<this>:(ref $Derived)=(ref $Derived), $i:i32=1:i32 (23, 23, 23, 23, 23, 23)
// test.kt:4 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived) (0, 15, 15, 15)
// test.kt:5 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p:i32=3:i32, $a:i32=0:i32 (25)
// test.kt:6 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p:i32=3:i32, $a:i32=0:i32 (16, 16)
// test.kt:7 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p:i32=3:i32, $a:i32=2:i32 (5, 5, 5)
// test.kt:13 $box: (4)
// test.kt:14 $box: (4, 12, 15, 4)
// test.kt:9 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p1:i32=4:i32, $p2:i32=5:i32 (35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35)
// test.kt:4 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived) (22, 22, 22, 22, 22, 0)
// test.kt:2 $Base.<init>: $<this>:(ref $Derived)=(ref $Derived), $i:i32=1:i32 (23, 23, 23, 23, 23, 23)
// test.kt:4 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived) (0, 15, 15, 15)
// test.kt:9 $Derived.<init>: $<this>:(ref $Derived)=(ref $Derived), $p1:i32=4:i32, $p2:i32=5:i32 (35, 41, 41, 41)
// test.kt:14 $box: (4)
// test.kt:15 $box: (1)
