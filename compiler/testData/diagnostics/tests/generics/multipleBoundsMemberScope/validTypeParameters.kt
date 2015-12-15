// !CHECK_TYPE

interface A {
    fun <T, E> foo(): E
}

interface B {
    fun <Q, W> foo(): W
}

fun <T> test(x: T) where T : B, T : A {
    x.foo<String, Int>().checkType { _<Int>() }
}
