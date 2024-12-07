// FILE: test.kt

val topLevelVal = ""
var topLevelVar = ""

class Foo {
    val memberVal = ""
    var memberVar = ""
}

fun box(): String {
    val foo0 = Foo()
    val foo1 = Foo()

    checkNotEqual(foo0::memberVal, Foo::memberVal)
    checkNotEqual(foo0::memberVal, foo1::memberVal)
    checkNotEqual(foo0::memberVar, Foo::memberVar)
    checkNotEqual(foo0::memberVar, foo1::memberVar)

    return "OK"
}

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}
