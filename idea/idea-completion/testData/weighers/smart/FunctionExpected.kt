fun foo(filter: (String) -> Boolean) {}

fun doFilter(s: String): Boolean = true
fun x(s: String): Boolean = true

fun bar(p: (String) -> Boolean) {
    foo(<caret>)
}

// ORDER: ::doFilter
// ORDER: p
// ORDER: {...}
// ORDER: { s -> ... }
// ORDER: { s: String -> ... }
// ORDER: ::x
// ORDER: ::TODO
// ORDER: ::error
