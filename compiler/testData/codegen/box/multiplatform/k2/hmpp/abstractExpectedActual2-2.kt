// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
expect abstract class AbsBase {
    abstract fun foo(): String
    fun bar(): String
}

// MODULE: lib-platform()()(lib-common)
actual abstract class AbsBase {
    actual abstract fun foo(): String
    actual fun bar(): String = "AbsBase"
}

class LibPlatformAbsBase : AbsBase() {
    override fun foo(): String = "LibPlatformAbsBase"
}

// MODULE: app-common(lib-common)
fun test_common(a: AbsBase) {
    a.foo()
    a.bar()
}

// MODULE: app-platform(lib-platform)()(app-common)
fun test_platform(a: AbsBase, b: LibPlatformAbsBase) {
    a.foo()
    b.foo()
    a.bar()
}

fun box(): String {
    return "OK"
}