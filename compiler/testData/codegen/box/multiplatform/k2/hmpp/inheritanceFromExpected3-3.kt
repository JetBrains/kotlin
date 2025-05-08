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

// MODULE: lib-inter()()(lib-common)
// FILE: libInter.kt

// MODULE: lib-platform()()(lib-inter)
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

// MODULE: app-inter(lib-common)()(app-common)
// FILE: appInter.kt

// MODULE: app-platform(lib-platform)()(app-inter)
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