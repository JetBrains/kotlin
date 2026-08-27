// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-88770

// MODULE: lib-common
abstract class Base {
    companion object {
        @JvmStatic
        protected val s: String = "OK"
    }
}

// MODULE: lib-platform()()(lib-common)


// MODULE: app-common(lib-common)
class Derived : Base() {
    fun foo(): String {
        return s
    }
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    return Derived().foo()
}
