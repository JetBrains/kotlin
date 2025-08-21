class Foo {
    class Bar
}

class C {
    val prop: Foo.Bar.Baz.
}

fun testFun() {
    val localProp: Foo.Bar.Baz.
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    lambdaParam: Foo.Bar.Baz. ->
}
