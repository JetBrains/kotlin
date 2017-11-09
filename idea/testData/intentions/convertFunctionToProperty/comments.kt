annotation class X(val s: String)

// 1
@X("") // 2
/* 3 */ fun foo<caret>(): String {
    // 4
    return ""
}