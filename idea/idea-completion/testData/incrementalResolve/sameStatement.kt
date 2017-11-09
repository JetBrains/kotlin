fun foo(p1: Int, p2: String): String {
    val v1 = p1.toString()
    val v2 = 123
    r<before><change>
}

// TYPE: "eturn "
// COMPLETION_TYPE: SMART
// ABSENT: p1
// EXIST: p2
// EXIST: v1
// ABSENT: v2
