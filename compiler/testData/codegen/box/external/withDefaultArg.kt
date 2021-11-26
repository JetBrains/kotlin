// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB
// FULL_JDK

package foo

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
        if (e.message != "foo.ObjWithNative.bar(JLjava/lang/String;)D" &&
            e.message != "'double foo.ObjWithNative.bar(long, java.lang.String)'") return "Fail 1: " + e.message
    }

    try {
        d = ObjWithNative.foo()
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "foo.ObjWithNative.foo(I)D" &&
            e.message != "'double foo.ObjWithNative.foo(int)'") return "Fail 2: " + e.message
    }

    try {
        d = topLevel()
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "foo.WithDefaultArgKt.topLevel(I)D" &&
            e.message != "'double foo.WithDefaultArgKt.topLevel(int)'") return "Fail 3: " + e.message
    }
    return "OK"
}
