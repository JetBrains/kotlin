// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class A
expect class B

expect interface Base1 {
    open fun foo(x: A): String
}

expect interface Base2 {
    fun foo(x: B): String
}

abstract class Derived : Base1, Base2 {
    abstract override fun foo(x: A): String
}

// MODULE: lib-platform()()(lib-common)
class C

actual typealias A = C
actual typealias B = C

actual interface Base1 {
    actual fun foo(x: C): String = "Base1"
}

actual interface Base2 {
    actual fun foo(x: C): String
}

class Impl : Derived(), Base1, Base2 {
    override fun foo(x: C): String {
        return "Impl" + super<Base1>.foo(x)
    }
}

// MODULE: app-common(lib-common)
fun useCommon(d: Derived, a: A, b: B, b1: Base1, b2: Base2): String {
    return d.foo(a) + b1.foo(a) + b2.foo(b)
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    val c = C()
    val impl = Impl()
    val fromCommon = useCommon(impl, c, c, impl, impl)

    return if (fromCommon == "ImplBase1ImplBase1ImplBase1") {
        "OK"
    } else {
        "FAIL"
    }
}
