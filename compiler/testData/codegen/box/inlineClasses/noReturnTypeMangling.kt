// WITH_STDLIB
// !LANGUAGE: -MangleClassMembersReturningInlineClasses

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val x: String)

class Test {
    fun getO() = S("O")
    val k = S("K")
}

fun box(): String {
    val t = Test()
    return t.getO().x + t.k.x
}