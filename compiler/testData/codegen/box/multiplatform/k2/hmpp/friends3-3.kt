// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-89391
// WITH_STDLIB

// MODULE: lib-common
internal fun foo() {}

// MODULE: lib-inter()()(lib-common)
// MODULE: lib-platform()()(lib-inter)


// MODULE: app-common()(lib-common)
fun test_common() {
    foo()
}

// MODULE: app-inter()(lib-inter, lib-common)(app-common)
fun test_inter() {
    foo()
}

// MODULE: app-platform()(lib-platform)(app-inter)
fun test_platform() {
    foo()
}

fun box(): String {
    return "OK"
}
