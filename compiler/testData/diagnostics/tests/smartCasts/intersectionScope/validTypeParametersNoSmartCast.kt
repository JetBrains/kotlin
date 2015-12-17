// !CHECK_TYPE

interface A {
    fun <T, E> foo(): E
}

interface B : A {
    override fun <Q, W> foo(): W
}

fun test(a: A) {
    if (a is B) {
        a.foo<String, Int>().checkType { _<Int>() }
    }
}
