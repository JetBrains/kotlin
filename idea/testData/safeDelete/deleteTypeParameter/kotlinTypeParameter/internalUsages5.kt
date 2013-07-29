class A<<caret>X> {
    fun foo<Y, Z>() where Y: X {

    }
}

fun bar(a: A<Number>) {
    a.foo<Int, String>()
}