interface A {
    context(c: T)
    fun <T> Int.foo(x: T)
}

interface B {
    context(c: T)
    fun <T> Int.foo(x: T)
}

class Y<caret> : A, B
