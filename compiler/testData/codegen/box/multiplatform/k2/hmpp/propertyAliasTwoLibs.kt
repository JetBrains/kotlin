// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class A
expect class B

expect open class Base1() { open val a: A }
expect interface Base2 { val b: B }

abstract class Derived : Base1(), Base2

// MODULE: lib-platform()()(lib-common)
class C(val t: String) {
    override fun toString(): String = t
}

actual typealias A = C
actual typealias B = C

actual open class Base1 actual constructor() {
    actual open val a: A
        get() = C("Base1a")
}

actual interface Base2 {
    actual val b: B
}

open class Impl1(
    override val a: C = C("Impl1a"),
    override val b: C = C("Impl1b"),
) : Derived()


// MODULE: lib2-common(lib-common)
class E

// MODULE: lib2-inter(lib-common)()(lib2-common)
typealias A2 = A
typealias B2 = B

// MODULE: lib2-platform(lib-platform)()(lib2-inter)
open class Impl2 : Base1(), Base2 {
    override val a: C
        get() = C("Impl2a")
    override val b: C
        get() = C("Impl2b")
}

// MODULE: app-common(lib-common)
fun useCommon(d: Derived, b1: Base1, b2: Base2): String {
    return d.a.toString() + d.b.toString() + b1.a.toString() + b2.b.toString()
}

// MODULE: app-inter(lib2-inter)(lib2-common)(app-common)
fun useInter(e: E) = e.toString()

// MODULE: app-platform(lib-platform, lib2-platform)()(app-inter)
class Combined(
    private val impl2: Impl2 = Impl2()
) : Impl1(C("a"), C("b")) {

    override val a: C
        get() = C("a")

    override val b: C
        get() = C("b")

    fun readAll(): String {
        return a.toString() + b.toString() + impl2.a.toString() + impl2.b.toString()
    }
}

fun box(): String {
    val combined = Combined()

    if (combined.readAll() != "abImpl2aImpl2b") {
        return "FAIL"
    }

    val res = useCommon(
        d = combined,
        b1 = combined,
        b2 = combined,
    )

    if (res != "abab") {
        return "FAIL"
    }

    return "OK"
}
