fun foo(x: Int): Int = x + 1

class A {
    fun bar(x: Int) = x + 1
}

class B(val x: Int) {
    val baz = x + 1
}