fun main() {
    val a = true
    inlineFun(a)
}

inline fun inlineFun(condition: Boolean) {
    if (condition) println("hello") // ← breakpoint, eval `condition`
}
// JVM_IR_TEMPLATES
// 1 condition\$iv

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE condition\\1 Z L2 L5 1