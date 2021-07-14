// TARGET_BACKEND: JVM
// WITH_RUNTIME

sealed class A
object B : A()
object C : A()

fun x(): A = C

fun test1() {
    // must be true
    println(B == B)
    // must be eqeqeq
    println(B == x())
}


sealed class D {
    override fun equals(other: Any?): Boolean = false
}

object E : D()

fun test2() {
    // must be eqeq
    println(E == E)
}


object F {
    override fun equals(other: Any?): Boolean = false
}

fun test3() {
    // must be eqeq
    println(F == F)
}

// 1 ICONST_1\s*ISTORE 0
// 1 IF_ACMPNE
// 2 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual
