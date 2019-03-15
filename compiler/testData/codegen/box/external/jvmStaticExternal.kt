// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FULL_JDK

package foo

class WithNative {
    companion object {
        @JvmStatic external fun bar(l: Long, s: String): Double
    }
}

object ObjWithNative {
    @JvmStatic external fun bar(l: Long, s: String): Double
}

fun box(): String {
    var d = 0.0
    try {
        d = WithNative.bar(1, "")
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "foo.WithNative.bar(JLjava/lang/String;)D") return "Fail 1: " + e.message
    }

    try {
        d = ObjWithNative.bar(1, "")
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "foo.ObjWithNative.bar(JLjava/lang/String;)D") return "Fail 2: " + e.message
    }
    return "OK"
}
