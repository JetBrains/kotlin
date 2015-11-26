class A<X> {
    fun <Y, <caret>Z> foo() {

    }
}

fun bar(a: A<String>) {
    a.foo<Int, Any>()
}