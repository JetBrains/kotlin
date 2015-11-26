class A<X> {
    fun <<caret>Y, Z> foo() {

    }
}

fun bar(a: A<String>) {
    a.foo<Int, Any>()
}