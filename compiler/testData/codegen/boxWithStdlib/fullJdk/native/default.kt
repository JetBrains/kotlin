package foo

import kotlin.jvm.*

object ObjWithNative {
    external fun foo(x: Int = 1): Double

    @JvmStatic external fun bar(l: Long, s: String = ""): Double
}

external fun topLevel(x: Int = 1): Double

fun box(): String {
    var d = 0.0

    try {
        d = ObjWithNative.bar(1)
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.getMessage() != "foo.ObjWithNative.bar(JLjava/lang/String;)D") return "Fail 1: " + e.getMessage()
    }

    try {
        d = ObjWithNative.foo()
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.getMessage() != "foo.ObjWithNative.foo(I)D") return "Fail 2: " + e.getMessage()
    }

    try {
        d = topLevel()
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.getMessage() != "foo.DefaultKt.topLevel(I)D") return "Fail 3: " + e.getMessage()
    }
    return "OK"
}