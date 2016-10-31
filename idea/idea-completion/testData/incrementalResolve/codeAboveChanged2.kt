fun foo(p: Int, p1: String): Int {
    val v1 = p<change>
    val v2 = 123
    return <before><caret>
}

// TYPE: "1"
// COMPLETION_TYPE: SMART
// ABSENT: v1
// EXIST: v2
// EXIST: p
// ABSENT: p1
