// LANGUAGE: +InlineClasses -MangleClassMembersReturningInlineClasses

inline class S(val x: String)

class Test {
    fun getO() = S("O")
    val k = S("K")
}

fun box(): String {
    val t = Test()
    return t.getO().x + t.k.x
}

// 1 public final getO\(\)Ljava/lang/String;
// 1 public final getK\(\)Ljava/lang/String;