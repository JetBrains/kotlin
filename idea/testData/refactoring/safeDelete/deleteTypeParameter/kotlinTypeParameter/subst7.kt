class A<X> {
    fun foo<<caret>Y>() {

    }
}

fun bar(a: A<String>) {
    a.foo<Int>()
}