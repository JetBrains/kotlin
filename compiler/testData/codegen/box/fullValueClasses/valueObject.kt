// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// WITH_STDLIB

value object Foo {
    val x: Int get() = 42
    fun member(): String = "member"
}

value object Bar

fun box(): String {
    if (Foo.x != 42) return "Fail: Foo.x == ${Foo.x}"
    if (Foo.member() != "member") return "Fail: Foo.member() == ${Foo.member()}"

    if (Foo != Foo) return "Fail: Foo != Foo"
    if (Bar != Bar) return "Fail: Bar != Bar"

    val anyFoo: Any = Foo
    val anyBar: Any = Bar
    if (anyFoo == anyBar) return "Fail: Foo == Bar"

    if (Foo.hashCode() != Foo.hashCode()) return "Fail: inconsistent hashCode"
    if (Foo.toString() != Foo.toString()) return "Fail: inconsistent toString"

    return "OK"
}
