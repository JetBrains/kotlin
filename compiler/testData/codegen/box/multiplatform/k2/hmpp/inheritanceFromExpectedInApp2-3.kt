// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect open class LibClass1() {
    open fun foo(): String
    fun bar()
}

expect open class LibClass2() {
    open fun foo(): String
    fun bar()
}

// MODULE: lib-platform()()(lib-common)
actual open class LibClass1 {
    actual open fun foo(): String = "OK"
    actual fun bar() {}
    fun baz(): String = "BAZ"
}

actual typealias LibClass2 = LibClass1

// MODULE: app-common(lib-common)
class AppLibClass1 : LibClass1() {
    override fun foo(): String = "AppCommon1"
}
class AppLibClass2 : LibClass2() {
    override fun foo(): String = "AppCommon2"
}

fun test_common(
    lc1: LibClass1,
    lc2: LibClass2,
    a1: AppLibClass1,
    a2: AppLibClass2,
) {
    lc1.bar()
    lc2.bar()
    lc1.foo()
    lc2.foo()
    a1.bar()
    a2.bar()
    a1.foo()
    a2.foo()
}

// MODULE: app-inter(lib-common)()(app-common)
class AppInterCommon1 : LibClass1() {
    override fun foo(): String = "AppInterCommon1"
}

class AppInterCommon2 : LibClass2() {
    override fun foo(): String = "AppInterCommon2"
}

fun test_platform(
    lc1: LibClass1,
    lc2: LibClass2,
    a1: AppLibClass1,
    a2: AppLibClass2,
    ai1: AppInterCommon1,
    ai2: AppInterCommon2
) {
    lc1.bar()
    lc2.bar()
    lc1.foo()
    lc2.foo()
    a1.bar()
    a2.bar()
    a1.foo()
    a2.foo()
    ai1.bar()
    ai2.bar()
    ai1.foo()
    ai2.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
class AppPlatformClass1 : LibClass1() {
    override fun foo(): String = "AppPlatform1"
    fun extra1(): String = baz()
}
class AppPlatformClass2 : LibClass2() {
    override fun foo(): String = "AppPlatform2"
    fun extra2(): String = baz()
}

fun test_platform(
    lc1: LibClass1,
    lc2: LibClass2,
    a1: AppLibClass1,
    a2: AppLibClass2,
    ap1: AppPlatformClass1,
    ap2: AppPlatformClass1
) {
    lc1.bar()
    lc2.bar()
    lc1.baz()
    lc2.baz()
    a1.bar()
    a2.bar()
    ap1.bar()
    ap2.bar()
    ap1.baz()
    ap2.baz()
}

fun box(): String {
    if (LibClass1().foo() != "OK") return "FAIL"
    if (LibClass2().foo() != "OK") return "FAIL"

    if (AppLibClass1().foo() != "AppCommon1") return "FAIL"
    if (AppLibClass2().foo() != "AppCommon2") return "FAIL"

    if (AppPlatformClass1().foo() != "AppPlatform1") return "FAIL"
    if (AppPlatformClass1().extra1() != "BAZ") return "FAIL"
    if (AppPlatformClass2().foo() != "AppPlatform2") return "FAIL"
    if (AppPlatformClass2().extra2() != "BAZ") return "FAIL"

    return "OK"
}
