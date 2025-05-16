
class Foo {
    class Bar
}

class C {
    val prop: Foo.Bar.Baz.Bazzzz
}

fun testFun() {
    val localProp: Foo.Bar.Baz.Bazzzz
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    lambdaParam: Foo.Bar.Baz.Bazzzz ->
}
