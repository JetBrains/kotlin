// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }

// MODULE: lib-platform()()(lib-common)
actual class LibClass1{ actual fun foo(): String = "OK" }
actual typealias LibClass2 = LibClass1

// MODULE: app-common(lib-common)
fun test_common(lc1: LibClass1, lc2: LibClass2) {
    lc1.foo()
    lc2.foo()
}

// MODULE: app-inter(lib-common)()(app-common)
fun test_inter(lc1: LibClass1, lc2: LibClass2) {
    lc1.foo()
    lc2.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(lc1: LibClass1, lc2: LibClass2) {
    lc1.foo()
    lc2.foo()
}

fun box(): String {
    return "OK"
}