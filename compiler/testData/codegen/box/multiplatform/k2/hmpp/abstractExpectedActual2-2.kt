// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: lib-common
// FILE: libCommon.kt
expect abstract class AbsBase {
    abstract fun foo(): String
    fun bar(): String
}

// MODULE: lib-platform()()(lib-common)
// FILE: libPlatform.kt
actual abstract class AbsBase {
    actual abstract fun foo(): String
    actual fun bar(): String = "AbsBase"
}

class LibPlatformAbsBase : AbsBase() {
    override fun foo(): String = "LibPlatformAbsBase"
}

// MODULE: app-common(lib-common)
// FILE: appCommon.kt
fun test_common(a: AbsBase) {
    a.foo()
    a.bar()
}

// MODULE: app-platform(lib-platform)()(app-common)
// FILE: appPlatform.kt
fun test_platform(a: AbsBase, b: LibPlatformAbsBase) {
    a.foo()
    b.foo()
    a.bar()
}

fun box(): String {
    return "OK"
}