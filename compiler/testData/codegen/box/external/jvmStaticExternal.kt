// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB
// FULL_JDK

package foo

class WithNative {
    companion object {
        @JvmStatic external fun bar(l: Long, s: String): Double

        @JvmStatic val prop: String external get
    }
}

object ObjWithNative {
    @JvmStatic external fun bar(l: Long, s: String): Double

    @JvmStatic val prop: String external get
}

fun check(vararg allowed: String, block: () -> Unit) {
    try {
        block()
        throw AssertionError("UnsatisfiedLinkError expected")
    } catch (e: java.lang.UnsatisfiedLinkError) {
        if (allowed.none { it == e.message }) {
            throw AssertionError("fail: ${e.message}")
        }
    }
}

fun box(): String {
    check(
        "foo.WithNative.bar(JLjava/lang/String;)D",
        "'double foo.WithNative.bar(long, java.lang.String)'"
    ) { WithNative.bar(1, "") }
    check(
        "foo.ObjWithNative.bar(JLjava/lang/String;)D",
        "'double foo.ObjWithNative.bar(long, java.lang.String)'"
    ) { ObjWithNative.bar(1, "") }
    check(
        "foo.WithNative.getProp()Ljava/lang/String;",
        "'java.lang.String foo.WithNative.getProp()'"
    ) { WithNative.prop }
    check(
        "foo.ObjWithNative.getProp()Ljava/lang/String;",
        "'java.lang.String foo.ObjWithNative.getProp()'"
    ) { ObjWithNative.prop }
    return "OK"
}
