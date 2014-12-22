val v = 1

fun foo([i<caret>) { }

// INVOCATION_COUNT: 0
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
