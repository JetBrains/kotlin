// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
// FILE: libCommon.kt
expect open class AbsBase {
    open fun foo(): String
    fun bar(): String
}

// MODULE: lib-platform()()(lib-common)
// FILE: libPlatform.kt
actual open class AbsBase {
    actual open fun foo(): String = "open AbsBase"
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