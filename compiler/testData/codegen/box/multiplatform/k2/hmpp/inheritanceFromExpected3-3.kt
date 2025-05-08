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

// MODULE: lib-inter()()(lib-common)
expect open class LibInterClass() {
    open fun foo(): String
    fun bar()
}

class LibInterClassDefault : LibInterClass() {
    override fun foo(): String = "idefault1"
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
fun test_common(
    lc1: LibClass1,
    lc2: LibClass2,
    lcd1: LibClassDefault1,
    lcd2: LibClassDefault2,
) {
    lc1.foo()
    lc2.foo()
    lc1.bar()
    lc2.bar()
    lcd1.foo()
    lcd2.foo()
    lcd1.bar()
    lcd2.bar()
}

// MODULE: app-inter(lib-inter)()(app-common)
fun test_inter(
    lc1: LibClass1,
    lic: LibInterClass,
    licd: LibInterClassDefault
) {
    lc1.foo()
    lc1.bar()
    lic.foo()
    lic.bar()
    licd.foo()
    licd.bar()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(
    lc1: LibClass1,
    lc2: LibClass2,
    lic: LibInterClass,
    lcd1: LibClassDefault1,
    lcd2: LibClassDefault2,
    licd: LibInterClassDefault
) {
    lc1.bar()
    lc2.bar()
    lcd1.bar()
    lcd2.bar()
    lic.bar()
    lcd1.baz()
    lcd2.baz()
    lic.baz()
    licd.bar()
    licd.baz()
}

fun box(): String {
    if (LibClassDefault1().foo() != "default1") return "FAIL"
    if (LibClassDefault2().foo() != "default2") return "FAIL"
    if (LibInterClassDefault().foo() != "idefault1") return "FAIL"
    if (LibClass1().foo() != "1") return "FAIL1"
    if (LibClass2().foo() != "2") return "FAIL2"
    if (LibInterClass().foo() != "3") return "FAIL"
    return "OK"
}