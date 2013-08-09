class A<X> {
    fun foo<Y, <caret>Z>() {

    }
}

fun bar(a: A<String>) {
    a.foo<Int, Any>()
}