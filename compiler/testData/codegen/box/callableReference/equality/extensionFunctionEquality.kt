fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

class Foo

fun Foo.topLevelExtension(): Unit {}

fun box(): String {
    val foo = Foo()
    val bar = Foo()

    // Unbound extension equality
    checkEqual(Foo::topLevelExtension, Foo::topLevelExtension)

    // Bound extension equality
    checkEqual(foo::topLevelExtension, foo::topLevelExtension)

    // Different-receiver bound inequality
    checkNotEqual(foo::topLevelExtension, bar::topLevelExtension)

    // Bound vs unbound inequality
    checkNotEqual(foo::topLevelExtension, Foo::topLevelExtension)

    return "OK"
}
