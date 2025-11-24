// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class A
expect class B

expect open class OverBase() {
    fun foo(x: A): String
    fun foo(x: B): String
}

// MODULE: lib-inter()()(lib-common)
typealias AInter = A
typealias BInter = B

open class InterBase : OverBase()

// MODULE: lib-platform()()(lib-inter)
class C1
class C2

actual typealias A = C1
actual typealias B = C2

actual open class OverBase actual constructor() {
    actual fun foo(x: A): String = "A"
    actual fun foo(x: B): String = "B"
}

class Impl : InterBase()

// MODULE: app-common(lib-common)
fun testCommon(base: OverBase, a: A, b: B): String {
    return base.foo(a) + base.foo(b)
}

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun testInter(base: InterBase, a: AInter, b: BInter): String {
    return base.foo(a) + base.foo(b)
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun box(): String {
    val a = C1()
    val b = C2()
    val impl = Impl()

    val fromCommon = testCommon(impl, a, b)
    if (fromCommon != "AB") return "Fail"

    val fromInter = testInter(impl, a, b)
    if (fromInter != "AB") return "Fail"

    return "OK"
}
