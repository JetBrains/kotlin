// WITH_STDLIB
// LANGUAGE: -MangleClassMembersReturningInlineClasses +ValueClasses, +GenericInlineClassParameter
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T)

class Test {
    fun getO() = S("O")
    val k = S("K")
}

fun box(): String {
    val t = Test()
    return t.getO().x + t.k.x
}