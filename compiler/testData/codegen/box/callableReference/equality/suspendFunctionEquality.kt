fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

suspend fun topLevelSuspendFun1(p: String) {}

suspend fun topLevelSuspendFun2(p: String) {}

class Foo {
    suspend fun memberSuspendFun(p: String) {}
}

fun box(): String {
    val foo = Foo()
    val bar = Foo()

    // Top-level suspend function equality
    checkEqual(::topLevelSuspendFun1, ::topLevelSuspendFun1)
    checkNotEqual(::topLevelSuspendFun1, ::topLevelSuspendFun2)

    // Bound suspend member function equality/inequality
    checkEqual(foo::memberSuspendFun, foo::memberSuspendFun)
    checkNotEqual(foo::memberSuspendFun, bar::memberSuspendFun)
    checkNotEqual(foo::memberSuspendFun, Foo::memberSuspendFun)

    return "OK"
}
