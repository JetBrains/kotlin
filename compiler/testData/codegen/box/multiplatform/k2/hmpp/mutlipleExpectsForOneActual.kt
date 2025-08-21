// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect class A
expect class B

expect open class Base1() {
    fun foo(x: A)
}

expect interface Base2 {
    fun foo(x: B)
}

abstract class Derived : Base1(), Base2


// MODULE: lib-platform()()(lib-common)

class C
actual typealias A = C
actual typealias B = C

actual open class Base1 actual constructor() {
    actual fun foo(x: C) {}
}

actual interface Base2 {
    actual fun foo(x: C)
}

class Impl : Derived()

// MODULE: app-common(lib-common)

fun testCommonUsage(a: A, b: B, base1: Base1, base2: Base2, derived: Derived) {
    base1.foo(a)
    base2.foo(b)
    derived.foo(a)
    derived.foo(b)
}

// MODULE: app-platform(lib-platform)()(app-common)

class AppDerived : Base1()

fun testPlatformUsage(c: C, base1: Base1, base2: Base2, derived: Derived, impl: Impl, appDerived: AppDerived) {
    base1.foo(c)
    base2.foo(c)
    derived.foo(c)
    impl.foo(c)
    appDerived.foo(c)
}

fun box(): String {
    val c = C()
    val impl = Impl()
    testCommonUsage(c, c, impl, impl, impl)
    testPlatformUsage(c, impl, impl, impl, impl, AppDerived())

    return "OK"
}
