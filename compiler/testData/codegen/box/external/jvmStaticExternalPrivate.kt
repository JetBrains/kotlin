// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB
// FULL_JDK

class C {
    companion object {
        private @JvmStatic external fun foo()
    }

    fun bar() {
        foo()
    }
}

fun box(): String {
    try {
        C().bar()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "C.foo()V" && e.message != "'void C.foo()'") return "Fail 1: " + e.message
    }

    return "OK"
}
