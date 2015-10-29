import kotlin.jvm.*

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
        if (e.getMessage() != "C.foo()V") return "Fail 1: " + e.getMessage()
    }

    return "OK"
}