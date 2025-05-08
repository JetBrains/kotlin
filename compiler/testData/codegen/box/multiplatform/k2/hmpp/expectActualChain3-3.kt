// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }
expect class LibClass3 { fun foo(): String }
expect class LibClass4 { fun foo(): String }
expect class LibClass5 { fun foo(): String }
expect class LibClass6 { fun foo(): String }
expect class LibClass7 { fun foo(): String }

// MODULE: lib-inter()()(lib-common)
expect class LibInterClass1 { fun foo(): String }
expect class LibInterClass2 { fun foo(): String }
expect class LibInterClass3 { fun foo(): String }
expect class LibInterClass4 { fun foo(): String }

actual class LibClass2 { actual fun foo(): String = "2" }
actual typealias LibClass3 = LibInterClass3
actual class LibClass4 { actual fun foo(): String = "4" }
actual typealias LibClass6 = LibClass2

// MODULE: lib-platform()()(lib-inter)
actual class LibClass1 { actual fun foo(): String = "1" }
actual typealias LibInterClass1 = LibClass1
actual typealias LibInterClass2 = LibClass2
actual class LibInterClass3 { actual fun foo(): String = "3" }
actual typealias LibInterClass4 = LibInterClass3
actual class LibClass5 { actual fun foo(): String = "5" }
actual typealias LibClass7 = LibClass1

// MODULE: app-common(lib-common)
fun test_common(lc1: LibClass1, lc2: LibClass2, lc4: LibClass4, lc5: LibClass5, lc7: LibClass7) {
    lc1.foo()
    lc2.foo()
    lc4.foo()
    lc5.foo()
    lc7.foo()
}

// MODULE: app-inter(lib-inter)()(app-common)
fun test_inter(lc2: LibClass2, lc4: LibClass4, lic1: LibInterClass1, lic2: LibInterClass2, lic3: LibInterClass3, lic4: LibInterClass4) {
    lc2.foo()
    lc4.foo()
    lic1.foo()
    lic2.foo()
    lic3.foo()
    lic4.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(lc1: LibClass1, lc2: LibClass2, lc4: LibClass4, lc5: LibClass5, lc7: LibClass7, lic1: LibInterClass1, lic2: LibInterClass2, lic3: LibInterClass3, lic4: LibInterClass4) {
    lc1.foo()
    lc2.foo()
    lc4.foo()
    lc5.foo()
    lc7.foo()
    lic1.foo()
    lic2.foo()
    lic3.foo()
    lic4.foo()
}

fun box(): String {
    return "OK"
}