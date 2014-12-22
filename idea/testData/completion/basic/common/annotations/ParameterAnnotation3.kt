val v = 1

fun foo(p: String, <caret>) { }

// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
