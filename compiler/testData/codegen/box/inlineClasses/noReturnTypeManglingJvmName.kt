// !LANGUAGE: +InlineClasses -MangleClassMembersReturningInlineClasses
// WITH_STDLIB
// TARGET_BACKEND: JVM

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val x: String)

class Test {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getO")
    fun getOK() = S("OK")
}

fun box(): String {
    val t = Test()
    return t.getOK().x
}