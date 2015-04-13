val v = 1

fun foo([volatile i<caret>) { }

// INVOCATION_COUNT: 0
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
