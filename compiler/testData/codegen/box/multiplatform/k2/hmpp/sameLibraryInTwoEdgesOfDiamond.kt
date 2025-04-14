// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_HMPP: JVM_IR

// MODULE: lib-common
// MODULE: lib-inter()()(lib-common)
fun foo() {}

// MODULE: lib-platform()()(lib-inter)


// MODULE: app-common(lib-common)
// MODULE: app-inter1(lib-common)()(app-common)
fun test_inter1() {
    foo()
}

// MODULE: app-inter2(lib-common)()(app-common)
fun test_inter2() {
    foo()
}

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
fun test_platform() {
    foo()
}

fun box() = "OK"
