// "Add non-null asserted (!!) call" "true"
class Foo {
    val bar = Bar()
}

class Bar {
    fun f() = 1
}

fun test(foo: Foo?) {
    val f = foo?.bar::f<caret>
}