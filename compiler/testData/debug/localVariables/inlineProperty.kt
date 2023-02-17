// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
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
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:10 box:
// test.kt:11 box: a:A=A
// test.kt:6 box: a:A=A, this_$iv:A=A, $i$f$getS:int=0:int
// test.kt:11 box: a:A=A
// test.kt:12 box: a:A=A, y:int=1:int
// test.kt:13 box: a:A=A, y:int=2:int

// EXPECTATIONS JS_IR
// test.kt:10 box:
// test.kt:4 <init>:
// test.kt:6 box: a=A
// test.kt:11 box: a=A
// test.kt:12 box: a=A, y=1:number
// test.kt:12 box: a=A, y=1:number
// test.kt:13 box: a=A, y=2:number
