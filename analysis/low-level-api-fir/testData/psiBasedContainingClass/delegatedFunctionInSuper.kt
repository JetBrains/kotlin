// WITH_STDLIB

interface A {
    fun foo()
}

fun a(): A = TODO()

object Outer {
    private object B : A by a() {}
}
