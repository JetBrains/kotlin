// TARGET_BACKEND: JVM

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

fun check(androidAllowed: String, vararg allowed: String, block: () -> Unit) {
    try {
        block()
        throw AssertionError("UnsatisfiedLinkError expected")
    } catch (e: java.lang.UnsatisfiedLinkError) {
        if (!e.message!!.contains(androidAllowed) && allowed.none { it == e.message }) {
            throw AssertionError("fail: ${e.message}")
        }
    }
}

fun box(): String {
    check(
        "No implementation found for double foo.WithNative.bar(long, java.lang.String)",
        "foo.WithNative.bar(JLjava/lang/String;)D",
        "'double foo.WithNative.bar(long, java.lang.String)'"
    ) { WithNative.bar(1, "") }
    check(
        "No implementation found for double foo.ObjWithNative.bar(long, java.lang.String)",
        "foo.ObjWithNative.bar(JLjava/lang/String;)D",
        "'double foo.ObjWithNative.bar(long, java.lang.String)'"
    ) { ObjWithNative.bar(1, "") }
    check(
        "No implementation found for java.lang.String foo.WithNative.getProp()",
        "foo.WithNative.getProp()Ljava/lang/String;",
        "'java.lang.String foo.WithNative.getProp()'"
    ) { WithNative.prop }
    check(
        "No implementation found for java.lang.String foo.ObjWithNative.getProp()",
        "foo.ObjWithNative.getProp()Ljava/lang/String;",
        "'java.lang.String foo.ObjWithNative.getProp()'"
    ) { ObjWithNative.prop }
    return "OK"
}
