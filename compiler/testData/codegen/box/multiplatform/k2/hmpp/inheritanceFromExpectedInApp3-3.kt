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

// MODULE: lib-inter()()(lib-common)
expect open class LibInterClass() {
    open fun foo(): String
    fun bar()
}

actual open class LibClass1 {
    actual open fun foo(): String = "1"
    actual fun bar() {}
    fun baz() {}
}

// MODULE: lib-platform()()(lib-inter)
actual open class LibClass2 {
    actual open fun foo(): String = "2"
    actual fun bar() {}
    fun baz() {}
}

actual open class LibInterClass {
    actual open fun foo(): String = "3"
    actual fun bar() {}
    fun baz() {}
}

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
    a2: AppLibClass2
) {
    lc1.foo()
    lc2.foo()
    lc1.bar()
    lc2.bar()
    a1.foo()
    a2.foo()
    a1.bar()
    a2.bar()
}

// MODULE: app-inter(lib-inter)()(app-common)
class AppInterCommon : LibInterClass() {
    override fun foo(): String = "AppInterCommon"
}

class AppLibClass3: LibClass1() {
    override fun foo(): String = "AppLibClass3"
}

class AppLibClass4 : LibClass1() {
    override fun foo(): String = "AppLibClass4"
}

fun test_inter(
    lc1: LibClass1,
    lic: LibInterClass,
    a1: AppLibClass1,
    a2: AppLibClass2,
    aic: AppInterCommon
) {
    lc1.foo()
    lc1.bar()
    aic.foo()
    aic.bar()
    lic.foo()
    lic.bar()
    a1.foo()
    a1.bar()
    a1.baz()
}

// MODULE: app-platform(lib-platform)()(app-inter)
class AppPlatformClass1 : LibClass1() {
    override fun foo(): String = "AppPlatform1"
    fun extra1() = baz()
}
class AppPlatformClass2 : LibClass2() {
    override fun foo(): String = "AppPlatform2"
    fun extra2() = baz()
}
class AppInterPlatform : LibInterClass() {
    override fun foo(): String = "AppInterPlatform"
    fun extra3() = baz()
}

fun test_platform(
    lc1: LibClass1,
    lc2: LibClass2,
    lic: LibInterClass,
    a1: AppLibClass1,
    a2: AppLibClass2,
    aic: AppInterCommon,
    ap1: AppPlatformClass1,
    ap2: AppPlatformClass2,
    aip: AppInterPlatform
) {
    lc1.foo()
    lc2.foo()
    lc1.bar()
    lc2.bar()
    lc1.baz()
    lc2.baz()
    aic.foo()
    aic.bar()
    aic.baz()
    lic.foo()
    lic.bar()
    lic.baz()
    ap1.bar()
    ap2.bar()
    ap1.baz()
    ap2.baz()
    ap1.extra1()
    ap2.extra2()
    a1.bar()
    a2.bar()
    a1.baz()
    a2.baz()
    aip.bar()
    aip.baz()
    aip.extra3()
}

fun box(): String {
    if (LibClass1().foo() != "1") return "FAIL"
    if (LibClass2().foo() != "2") return "FAIL"
    if (AppLibClass3().foo() != "AppLibClass3") return "FAIL"
    if (AppLibClass4().foo() != "AppLibClass4") return "FAIL"
    if (LibInterClass().foo() != "3") return "FAIL"
    if (AppLibClass1().foo() != "AppCommon1") return "FAIL"
    if (AppLibClass2().foo() != "AppCommon2") return "FAIL"
    if (AppInterCommon().foo() != "AppInterCommon") return "FAIL"
    if (AppPlatformClass1().foo() != "AppPlatform1") return "FAIL"
    if (AppPlatformClass2().foo() != "AppPlatform2") return "FAIL"
    if (AppInterPlatform().foo() != "AppInterPlatform") return "FAIL"
    return "OK"
}
