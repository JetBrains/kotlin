// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
// FILE: lib-common.kt
expect class CLib

expect class BaseCLib1 {
    fun foo()
    fun bar(x: CLib)
}

expect class BaseCLib2 {
    fun foo()
    fun bar(x: CLib)
}

// MODULE: lib-platform()()(lib-common)
// FILE: lib-platform.kt
class CPlatform

class BaseCPlatform {
    fun foo() {}
    fun bar(x: CPlatform) {}
}

actual typealias CLib = CPlatform

actual open class BaseCLib1 {
    actual fun foo() {}
    actual fun bar(x: CPlatform) {}
}

actual typealias BaseCLib2 = BaseCPlatform


// MODULE: app-common(lib-common)
// FILE: app-common.kt
fun test_common(a: CLib, base1: BaseCLib1, base2: BaseCLib2) {
    base1.foo()
    base1.bar(a)

    base2.foo()
    base2.bar(a)
}

fun appCommonFun(){}

// MODULE: app-platform(lib-platform)()(app-common)
// FILE: app-platform.kt

fun test_platformFun(a: CLib, b: CPlatform, base1: BaseCLib1, base2: BaseCLib2, base3: BaseCPlatform) {
    base1.foo()
    base1.bar(a)

    base2.foo()
    base2.bar(a)

    base3.foo()
    base3.bar(b)
}

fun box(): String {
    return "OK"
}