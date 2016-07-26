// Nothing can be cast to Nothing
fun foo(x: String) {
    x <!CAST_NEVER_SUCCEEDS!>as<!> Nothing
}

fun gav(y: String?) {
    y <!CAST_NEVER_SUCCEEDS!>as<!> Nothing
}

// Only nullable can be cast to Nothing?
fun bar(x: String, y: String?) {
    x <!CAST_NEVER_SUCCEEDS!>as<!> Nothing?
    y as Nothing?
}