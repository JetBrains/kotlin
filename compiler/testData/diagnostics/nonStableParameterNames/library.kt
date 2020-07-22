package library

fun foo(a: Int, b: String, c: Boolean) = Unit

class Foo(a: Int, b: String, c: Boolean) {
    constructor(a: Int, b: String, c: Boolean, d: Double) : this(a, b, c)

    fun foo(a: Int, b: String, c: Boolean) = Unit

    class Bar(a: Int, b: String, c: Boolean) {
        constructor(a: Int, b: String, c: Boolean, d: Double) : this(a, b, c)

        fun bar(a: Int, b: String, c: Boolean) = Unit
    }
}
