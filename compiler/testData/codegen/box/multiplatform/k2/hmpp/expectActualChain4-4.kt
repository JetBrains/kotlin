// LANGUAGE: +MultiPlatformProjects
// KT-77599 K2: HMPP compilation scheme: ClassCastException on typealias actualization

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }
expect class LibClass3 { fun foo(): String }
expect class LibClass4 { fun foo(): String }
expect class LibClass5 { fun foo(): String }
expect class LibClass6 { fun foo(): String }
expect class LibClass7 { fun foo(): String }
expect class LibClass8 { fun foo(): String }
expect class LibClass9 { fun foo(): String }

// MODULE: lib-inter1()()(lib-common)
expect class LibInterClass1 { fun foo(): String }
expect class LibInterClass2 { fun foo(): String }
expect class LibInterClass3 { fun foo(): String }
expect class LibInterClass4 { fun foo(): String }
expect class LibInterClass5 { fun foo(): String }
expect class LibInterClass6 { fun foo(): String }

actual class LibClass2 { actual fun foo(): String = "2" }
actual typealias LibClass3 = LibInterClass3
actual typealias LibClass6 = LibClass2

// MODULE: lib-inter2()()(lib-common)
expect class LibInterClass7 { fun foo(): String }
expect class LibInterClass8 { fun foo(): String }
expect class LibInterClass9 { fun foo(): String }
expect class LibInterClass10 { fun foo(): String }

actual class LibClass4 { actual fun foo(): String = "4" }
actual typealias LibClass9 = LibClass2
actual typealias LibClass8 = LibInterClass7
actual typealias LibClass5 = LibClass4

// MODULE: lib-platform()()(lib-inter1, lib-inter2)
actual class LibClass1 { actual fun foo(): String = "1" }
actual typealias LibInterClass1 = LibClass1
actual typealias LibInterClass2 = LibClass2
actual class LibInterClass3 { actual fun foo(): String = "3" }
actual typealias LibInterClass4 = LibInterClass3
actual class LibInterClass5 { actual fun foo(): String = "5" }
actual typealias LibClass7 = LibClass1
actual class LibInterClass7 { actual fun foo(): String = "5" }
actual typealias LibInterClass8 = LibInterClass7
actual typealias LibInterClass6 = LibClass4
actual typealias LibInterClass9 = LibClass2
actual typealias LibInterClass10 = LibClass4

// MODULE: app-common(lib-common)
fun test_common(lc1: LibClass1, lc2: LibClass2, lc3: LibClass3, lc4: LibClass4, lc5: LibClass5, lc6: LibClass6, lc7: LibClass7, lc8: LibClass8, lc9: LibClass9) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
    lc4.foo()
    lc5.foo()
    lc6.foo()
    lc7.foo()
    lc8.foo()
    lc9.foo()
}

// MODULE: app-inter1(lib-inter1)()(app-common)
fun test_inter1(lc2: LibClass2, lc3: LibClass3, lc6: LibClass6, lic1: LibInterClass1, lic2: LibInterClass2, lic3: LibInterClass3, lic4: LibInterClass4, lic5: LibInterClass5) {
    lc2.foo()
    lc3.foo()
    lc6.foo()
    lic1.foo()
    lic2.foo()
    lic3.foo()
    lic4.foo()
    lic5.foo()
}

// MODULE: app-inter2(lib-inter2)()(app-common)
fun test_inter2(lc4: LibClass4, lc5: LibClass5, lc8: LibClass8, lc9: LibClass9, lic7: LibInterClass7, lic8: LibInterClass8, lic9: LibInterClass9) {
    lc4.foo()
    lc5.foo()
    lc8.foo()
//    lc9.foo() // MISSING_DEPENDENCY_CLASS
    lic7.foo()
    lic8.foo()
    lic9.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
fun test_platform(lc1: LibClass1, lc2: LibClass2, lc3: LibClass3, lc4: LibClass4, lc5: LibClass5, lc6: LibClass6, lc7: LibClass7,  lc8: LibClass8,  lc9: LibClass9, lic1: LibInterClass1, lic2: LibInterClass2, lic3: LibInterClass3, lic4: LibInterClass4, lic5: LibInterClass5, lic6: LibInterClass6, lic7: LibInterClass7, lic8: LibInterClass8, lic9: LibInterClass9) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
    lc4.foo()
    lc5.foo()
    lc6.foo()
    lc7.foo()
    lc8.foo()
    lc9.foo()
    lic1.foo()
    lic2.foo()
    lic3.foo()
    lic4.foo()
    lic5.foo()
    lic6.foo()
    lic7.foo()
    lic8.foo()
    lic9.foo()
}

fun box(): String {
    return "OK"
}