// CHECK_TYPE

interface A {
    fun <T, E> foo(): E
}

interface B {
    fun <Q, W> foo(): W
}

fun test(c: Any) {
    if (c is B && c is A) {
        c.foo<String, Int>().checkType { _<Int>() }
    }
}
