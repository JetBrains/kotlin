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

// EXPECTATIONS JVM JVM_IR
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
// test.kt:9 box: a=A
// test.kt:10 box: a=A, y=1:number
// test.kt:10 box: a=A, y=1:number
// test.kt:11 box: a=A, y=2:number
