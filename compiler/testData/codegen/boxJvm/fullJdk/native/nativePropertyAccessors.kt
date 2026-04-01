// TARGET_BACKEND: JVM

// FULL_JDK
package test

class C {
    companion object {
        val defaultGetter: Int = 1
            external get

        var defaultSetter: Int = 1
            external get
            external set
    }

    val defaultGetter: Int = 1
        external get

    var defaultSetter: Int = 1
        external get
        external set
}

val defaultGetter: Int = 1
    external get

var defaultSetter: Int = 1
    external get
    external set

fun check(body: () -> Unit, signature: String, jdk11Signature: String, androidMessage: String): String? {
    try {
        body()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {
        if (e.message != signature && e.message != jdk11Signature && !e.message!!.contains(androidMessage)) return "Fail $signature: " + e.message
    }

    return null
}

fun box(): String {
    return check({defaultGetter}, "test.NativePropertyAccessorsKt.getDefaultGetter()I", "'int test.NativePropertyAccessorsKt.getDefaultGetter()'", "No implementation found for int test.NativePropertyAccessorsKt.getDefaultGetter()")
           ?: check({defaultSetter = 1}, "test.NativePropertyAccessorsKt.setDefaultSetter(I)V", "'void test.NativePropertyAccessorsKt.setDefaultSetter(int)'", "No implementation found for void test.NativePropertyAccessorsKt.setDefaultSetter(int)")

           ?: check({C.defaultGetter}, "test.C\$Companion.getDefaultGetter()I", "'int test.C\$Companion.getDefaultGetter()'", "No implementation found for int test.C\$Companion.getDefaultGetter()")
           ?: check({C.defaultSetter = 1}, "test.C\$Companion.setDefaultSetter(I)V", "'void test.C\$Companion.setDefaultSetter(int)'", "No implementation found for void test.C\$Companion.setDefaultSetter(int)")

           ?: check({C().defaultGetter}, "test.C.getDefaultGetter()I", "'int test.C.getDefaultGetter()'", "No implementation found for int test.C.getDefaultGetter()")
           ?: check({C().defaultSetter = 1}, "test.C.setDefaultSetter(I)V", "'void test.C.setDefaultSetter(int)'", "No implementation found for void test.C.setDefaultSetter(int)")

           ?: "OK"
}
