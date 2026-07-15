interface I {
    context(xx: Int)
    fun <T> Int.foo(x: T)
}

class Delegat<caret>ing(impl: I) : I by impl
