fun foo(p1: Any, p2: Any) {
    if (p1 !is String) return
    val v1 = "a"
    val v2 = 123
    println("a")
    bar(x<before><change>)
    if (p2 !is String) return
}

fun bar(s: String){}

// BACKSPACES: 1
// COMPLETION_TYPE: SMART
// EXIST: v1
// ABSENT: v2
// EXIST: p1
// ABSENT: p2
