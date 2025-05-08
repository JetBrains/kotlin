// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect open class LibClass1() {
    open fun foo(): String
    fun bar()
}

class LibClassDefault1 : LibClass1() {
    override fun foo(): String = "1"
}

expect open class LibClass2() {
    open fun foo(): String
    fun bar()
}
class LibClassDefault2 : LibClass2() {
    override fun foo(): String = "2"
}

// MODULE: lib-platform()()(lib-common)
actual open class LibClass1 {
    actual open fun foo(): String = "OK"
    actual fun bar() {}
    fun baz() {}
}
actual typealias LibClass2 = LibClass1

// MODULE: app-common(lib-common)
fun test_common(lc1: LibClass1, lc2: LibClass2, lcd1: LibClassDefault1, lcd2: LibClassDefault2) {
    lc1.bar()
    lc2.bar()
    lcd1.bar()
    lcd2.bar()
}

// MODULE: app-inter(lib-common)()(app-common)
fun test_inter(lc1: LibClass1, lc2: LibClass2, lcd1: LibClassDefault1, lcd2: LibClassDefault2) {
    lc1.foo()
    lc2.foo()
    lc1.bar()
    lc2.bar()
    lcd1.foo()
    lcd2.foo()
    lcd1.bar()
    lcd2.bar()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(lc1: LibClass1, lc2: LibClass2, lcd1: LibClassDefault1, lcd2: LibClassDefault2) {
    lc1.bar()
    lc2.bar()
    lc1.baz()
    lc2.baz()
    lcd1.bar()
    lcd2.bar()
    lcd1.baz()
    lcd2.baz()
}

fun box(): String {
    if (LibClassDefault1().foo() != "1") return "FAIL"
    if (LibClassDefault2().foo() != "2") return "FAIL"
    if (LibClass1().foo() != "OK") return "FAIL"
    if (LibClass2().foo() != "OK") return "FAIL"
    return "OK"
}