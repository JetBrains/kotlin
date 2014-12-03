fun foo(filter: (String) -> Boolean) {}

fun doFilter(s: String): Boolean = true
fun x(s: String): Boolean = true

fun bar(p: (String) -> Boolean) {
    foo(<caret>)
}

// ORDER: ::doFilter
// ORDER: p
// ORDER: countTo
// ORDER: {...}
// ORDER: { String -> ... }
// ORDER: ::error
// ORDER: ::x
