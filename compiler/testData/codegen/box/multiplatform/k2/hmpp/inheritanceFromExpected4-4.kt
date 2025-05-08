// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt

expect open class Base() {
    fun foo()
    open fun bar(): String
}

class BaseDefault : Base() {
    override fun bar(): String = "0"
}

// MODULE: lib-inter1()()(lib-common)
// FILE: libInter1.kt

// MODULE: lib-inter2()()(lib-common)
// FILE: libInter2.kt

// MODULE: lib-platform()()(lib-inter1, lib-inter2)
// FILE: libPlatform.kt

actual open class Base actual constructor() {
    actual fun foo() {}
    actual open fun bar(): String = "1"
    fun baz() {}
}

// MODULE: app-common(lib-common)
// FILE: appCommon.kt

fun test(b: BaseDefault) {
    b.foo()
    b.bar()
}

// MODULE: app-inter1(lib-inter1)()(app-common)
// FILE: appInter1.kt

// MODULE: app-inter2(lib-inter2)()(app-common)
// FILE: appInter2.kt

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
// FILE: appPlatform.kt

fun test2(b: BaseDefault) {
    b.foo()
    b.bar()
    b.baz()
}

fun box(): String {
    test(BaseDefault())
    test2(BaseDefault())
    return "OK"
}