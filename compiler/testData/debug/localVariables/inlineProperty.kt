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

// LOCAL VARIABLES
// TestKt:8:
// A:2:
// TestKt:8:
// TestKt:9: a:A=A
// TestKt:4: a:A=A, this_$iv:A=A, $i$f$getS:int=0:int
// TestKt:9: a:A=A
// TestKt:10: a:A=A, y:int=1:int
// TestKt:11: a:A=A, y:int=2:int