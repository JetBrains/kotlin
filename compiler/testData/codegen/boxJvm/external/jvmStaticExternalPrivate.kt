// TARGET_BACKEND: JVM

// FULL_JDK

package test

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
        if (e.message != "test.C.foo()V" && e.message != "'void test.C.foo()'" &&
            !e.message!!.contains("No implementation found for void test.C.foo()")) return "Fail 1: " + e.message
    }

    return "OK"
}
