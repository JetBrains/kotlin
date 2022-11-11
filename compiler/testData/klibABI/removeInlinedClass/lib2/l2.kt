open class Bar : Foo()

inline fun fooVariableType() {
    val foo: Foo? = null
    check(foo == null)
}

inline fun barVariableType() {
    val bar: Bar? = null
    check(bar == null)
}

inline fun fooInstance() {
    check(Foo().toString() != "Qux")
}

inline fun barInstance() {
    check(Bar().toString() != "Qux")
}

inline fun fooInstance2() {
    check(run(::Foo).toString() != "Qux")
}

inline fun barInstance2() {
    check(run(::Bar).toString() != "Qux")
}

inline fun fooAnonymousObject() {
    val foo = object : Foo() {}
    check(foo == null)
}

inline fun barAnonymousObject() {
    val bar = object : Bar() {}
    check(bar == null)
}
