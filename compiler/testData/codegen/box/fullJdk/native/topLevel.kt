// TARGET_BACKEND: JVM

// FULL_JDK

package foo

external fun bar(l: Long, s: String): Double

fun box(): String {
    var d = 0.0

    try {
        d = bar(1, "")
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != "foo.TopLevelKt.bar(JLjava/lang/String;)D") return "Fail 1: " + e.message
    }

    return "OK"
}
