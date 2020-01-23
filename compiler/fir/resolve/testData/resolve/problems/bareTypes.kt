interface A<out T>

interface MutableA<T> : A<T> {
    fun add(x: T)
}

fun test(a: A<String>) {
    (a as MutableA).<!INAPPLICABLE_CANDIDATE!>add<!>("")
}
