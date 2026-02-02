// FILE: test.kt
class A {
    inline val s: Int
        get() = 1
}

fun box() {
    val a = A()
    var y = a.s
    y++
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:8 box:
// test.kt:2 <init>:
// test.kt:8 box:
// test.kt:9 box: a:A=A
// test.kt:4 box: a:A=A, this_\1:A=A, $i$f$getS\1\9:int=0:int
// test.kt:9 box: a:A=A
// test.kt:10 box: a:A=A, y:int=1:int
// test.kt:11 box: a:A=A, y:int=2:int

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// test.kt:2 <init>:
// test.kt:8 box:
// test.kt:9 box: a:A=A
// test.kt:4 box: a:A=A, this_$iv:A=A, $i$f$getS:int=0:int
// test.kt:9 box: a:A=A
// test.kt:10 box: a:A=A, y:int=1:int
// test.kt:11 box: a:A=A, y:int=2:int

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:2 <init>:
// test.kt:4 box: a=A
// test.kt:10 box: a=A, y=1:number
// test.kt:11 box: a=A, y=2:number

// EXPECTATIONS WASM
// test.kt:8 $box: $a:(ref null $A)=null, $y:i32=0:i32 (12, 12)
// test.kt:5 $A.<init>: $<this>:(ref $A)=(ref $A) (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// test.kt:9 $box: $a:(ref $A)=(ref $A), $y:i32=0:i32 (12, 12, 14, 14, 14)
// test.kt:4 $box: $a:(ref $A)=(ref $A), $y:i32=0:i32 (16, 17)
// test.kt:10 $box: $a:(ref $A)=(ref $A), $y:i32=2:i32 (4, 4, 4, 5, 5, 5, 5, 5, 5, 4, 4, 4)
// test.kt:11 $box: $a:(ref $A)=(ref $A), $y:i32=2:i32 (1, 1)
