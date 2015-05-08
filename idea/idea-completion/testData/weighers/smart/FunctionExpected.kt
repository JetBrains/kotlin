fun foo(filter: (String) -> Boolean) {}

fun doFilter(s: String): Boolean = true
fun x(s: String): Boolean = true

fun bar(p: (String) -> Boolean) {
    foo(<caret>)
}

// ORDER: ::doFilter
// ORDER: p
// ORDER: {...}
// ORDER: { String -> ... }
// ORDER: ::x
// ORDER: ::error
