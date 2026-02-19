
class Foo

class C {
    val prop: Foo.Bar
}

fun testFun() {
    val localProp: Foo.Bar
}

fun <T> take(action: (T) -> Unit) {}

val prop = take {
    lambdaParam: Foo.Bar ->
}
