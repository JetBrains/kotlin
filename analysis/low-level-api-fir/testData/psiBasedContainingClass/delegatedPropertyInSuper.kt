// WITH_STDLIB

interface A {
    val kind: String
}

fun a(): A = TODO()

object Outer {
    object B : A by a() {}
}
