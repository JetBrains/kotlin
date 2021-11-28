fun foo(x: (Int) -> Unit) {}

class A {
    fun bar() {}
}

fun main(a: A?, y: String) {
    foo {
        a?.bar() ?: y.get(0)
    }
}
