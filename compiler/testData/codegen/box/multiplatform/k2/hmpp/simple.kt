// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
expect fun foo()
fun bar() {}

class A {
    fun foo() {}
}

expect class B {
    fun foo()
}

// MODULE: lib-platform()()(lib-common)
actual fun foo() {}
actual class B {
    actual fun foo() {}
    fun bar() {}
}

// MODULE: app-common(lib-common)
fun commonFun() {}

fun test_common(a: A, b: B) {
    a.foo()
    b.foo()
    bar()
}

// MODULE: app-inter(lib-common)()(app-common)
fun interFun() {}

fun test_inter(a: A, b: B) {
    a.foo()
    b.foo()
    commonFun()
    bar()
}

// MODULE: app-platform(lib-platform)()(app-inter)
fun test_platform(a: A, b: B) {
    a.foo()
    b.foo()
    b.bar()
    commonFun()
    interFun()
    bar()
}

fun box() = "OK"
