// WITH_STDLIB
// LANGUAGE: -MangleClassMembersReturningInlineClasses +ValueClasses
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String)

class Test {
    fun getO() = S("O")
    val k = S("K")
}

fun box(): String {
    val t = Test()
    return t.getO().x + t.k.x
}