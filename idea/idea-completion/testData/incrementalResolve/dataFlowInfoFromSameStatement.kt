fun foo(p: Any): String {
    println(1)
    if (<before><change>
}

fun bar(s: String): Boolean = true

// TYPE: "p is String && bar("
// COMPLETION_TYPE: SMART
// EXIST: p
