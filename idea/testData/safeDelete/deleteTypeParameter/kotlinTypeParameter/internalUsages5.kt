class A<<caret>X> {
    fun <Y, Z> foo() where Y: X {

    }
}

fun bar(a: A<Number>) {
    a.foo<Int, String>()
}