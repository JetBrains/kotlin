// class: Foo
// KT-90199
data class X(val a: Int, val b: String)

class Foo {
    val (a, b) = X(1, "")

    val (c, d) = X(2, "")

    val e = 3
}
