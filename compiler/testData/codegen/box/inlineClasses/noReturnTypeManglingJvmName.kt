// !LANGUAGE: +InlineClasses -MangleClassMembersReturningInlineClasses
// WITH_RUNTIME
// TARGET_BACKEND: JVM

inline class S(val x: String)

class Test {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getO")
    fun getOK() = S("OK")
}

fun box(): String {
    val t = Test()
    return t.getOK().x
}