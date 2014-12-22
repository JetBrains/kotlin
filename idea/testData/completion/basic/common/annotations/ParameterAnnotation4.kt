val v = 1

fun foo(volatile <caret>) { }

// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
