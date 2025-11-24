// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

expect class A
expect class B

expect open class Base1() {
    open val a: A
}

expect interface Base2 {
    val b: B
}

abstract class Derived : Base1(), Base2


// MODULE: lib-inter()()(lib-common)

typealias AInter = A
typealias BInter = B

abstract class InterDerived : Derived()

// MODULE: lib-platform()()(lib-inter)
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

class Impl : InterDerived() {
    override val a: C
        get() = C("Impla")
    override val b: C
        get() = C("Implb")
}


// MODULE: app-common(lib-common)

fun useCommon(d: Derived, b1: Base1, b2: Base2): String {
    return d.a.toString() + d.b.toString() + b1.a.toString() + b2.b.toString()
}

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun useInter(d: InterDerived, a: AInter, b: BInter): String {
    return useCommon(d, d, d)
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    val impl = Impl()
    val fromCommon = useCommon(impl, impl, impl)
    if (fromCommon != "ImplaImplbImplaImplb") {
        return "FAIL"
    }

    val fromInter = useInter(impl, C("a"), C("b"))
    if (fromInter != "ImplaImplbImplaImplb") {
        return "FAIL"
    }

    return "OK"
}
