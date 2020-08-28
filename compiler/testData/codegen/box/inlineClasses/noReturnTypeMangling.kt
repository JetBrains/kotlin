// !LANGUAGE: +InlineClasses -MangleClassMembersReturningInlineClasses

inline class S(val x: String)

class Test {
    fun getO() = S("O")
    val k = S("K")
}

fun box(): String {
    val t = Test()
    return t.getO().x + t.k.x
}