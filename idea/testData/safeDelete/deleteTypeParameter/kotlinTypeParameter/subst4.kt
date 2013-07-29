class A<X> {
    fun foo<<caret>Y, Z>() {

    }
}

fun bar(a: A<String>) {
    a.foo<Int, Any>()
}