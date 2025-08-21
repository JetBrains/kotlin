// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect open class LibClass1() {
    open fun foo(): String
    fun bar()
}
class LibClassDefault1 : LibClass1() {
    override fun foo(): String = "default1"
}

expect open class LibClass2() {
    open fun foo(): String
    fun bar()
}
class LibClassDefault2 : LibClass2() {
    override fun foo(): String = "default2"
}

// MODULE: lib-inter1()()(lib-common)
expect open class LibInterClass1() {
    open fun foo(): String
    fun bar()
}
class LibInterClass1Default : LibInterClass1() {
    override fun foo(): String = "idefault1"
}

actual open class LibClass1 {
    actual open fun foo(): String = "1"
    actual fun bar() {}
    fun baz() {}
}

// MODULE: lib-inter2()()(lib-common)
expect open class LibInterClass2() {
    open fun foo(): String
    fun bar()
}
class LibInterClass2Default : LibInterClass2() {
    override fun foo(): String = "idefault2"
}

actual open class LibClass2 {
    actual open fun foo(): String = "2"
    actual fun bar() {}
    fun baz() {}
}

// MODULE: lib-platform()()(lib-inter1, lib-inter2)
actual open class LibInterClass1 {
    actual open fun foo(): String = "3"
    actual fun bar() {}
    fun baz() {}
}

actual open class LibInterClass2 {
    actual open fun foo(): String = "4"
    actual fun bar() {}
    fun baz() {}
}

// MODULE: app-common(lib-common)
fun test_common(
    lc1: LibClass1,
    lc2: LibClass2,
    lcd1: LibClassDefault1,
    lcd2: LibClassDefault2
) {
    lc1.foo()
    lc1.bar()
    lc2.foo()
    lc2.bar()
    lcd1.foo()
    lcd1.bar()
    lcd2.foo()
    lcd2.bar()
}

// MODULE: app-inter1(lib-inter1)()(app-common)
fun test_inter1(
    lc1: LibClass1,
    lic1: LibInterClass1,
    lic1d: LibInterClass1Default
) {
    lc1.foo()
    lc1.bar()
    lic1.foo()
    lic1.bar()
    lic1d.foo()
    lic1d.bar()
}

// MODULE: app-inter2(lib-inter2)()(app-common)
fun test_inter2(
    lc2: LibClass2,
    lic2: LibInterClass2,
    lic2d: LibInterClass2Default
) {
    lc2.foo()
    lc2.bar()
    lic2.foo()
    lic2.bar()
    lic2d.foo()
    lic2d.bar()
}

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
fun test_platform(
    lc1: LibClass1,
    lc2: LibClass2,
    lic1: LibInterClass1,
    lic2: LibInterClass2,
    lcd1: LibClassDefault1,
    lcd2: LibClassDefault2,
    lic1d: LibInterClass1Default,
    lic2d: LibInterClass2Default
) {
    lc1.bar()
    lc2.bar()
    lcd1.bar()
    lcd2.bar()
    lic1.bar()
    lic2.bar()
    lic1d.bar()
    lic2d.bar()
    lc1.baz()
    lc2.baz()
    lic1.baz()
    lic2.baz()
    lic1d.baz()
    lic2d.baz()
}

fun box(): String {
    if (LibClassDefault1().foo() != "default1") return "FAIL"
    if (LibClassDefault2().foo() != "default2") return "FAIL"
    if (LibInterClass1Default().foo() != "idefault1") return "FAIL"
    if (LibInterClass2Default().foo() != "idefault2") return "FAIL"
    if (LibClass1().foo() != "1") return "FAIL"
    if (LibClass2().foo() != "2") return "FAIL"
    if (LibInterClass1().foo() != "3") return "FAIL"
    if (LibInterClass2().foo() != "4") return "FAIL"
    return "OK"
}