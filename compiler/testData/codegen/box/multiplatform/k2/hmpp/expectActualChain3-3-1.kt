// LANGUAGE: +MultiPlatformProjects
// KT-77599 K2: HMPP compilation scheme: ClassCastException on typealias actualization

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }
expect class LibClass3 { fun foo(): String }


// MODULE: lib-inter()()(lib-common)
expect class LibInterClass3 { fun foo(): String }

actual class LibClass1 { actual fun foo(): String = "1" }
actual typealias LibClass2 = LibClass1
actual typealias LibClass3 = LibInterClass3

// MODULE: lib-platform()()(lib-inter)
actual class LibInterClass3 { actual fun foo(): String = "2" }

// MODULE: app-common(lib-common)
fun test_common( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}

// MODULE: app-inter(lib-inter)()(app-common)
fun test_inter( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}
fun box(): String {
    return "OK"
}
