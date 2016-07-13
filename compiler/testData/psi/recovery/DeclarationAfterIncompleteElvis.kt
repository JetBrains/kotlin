fun foo(x: Any?) {
    x ?:
    val foo = 1

    x ?:
    fun bar() = 2

    x ?:
    fun String.() = 3
}

class A {
    val z = null ?:
    val x = 4

    val y = null ?:
    fun baz() = 5

    val q = null ?:
    fun String.() = 6
}
