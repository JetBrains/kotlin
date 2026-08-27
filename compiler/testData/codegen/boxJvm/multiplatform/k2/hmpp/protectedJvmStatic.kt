// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_HMPP: JVM_IR
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
        return <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC{PLATFORM}!>s<!>
    }
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    return Derived().foo()
}
