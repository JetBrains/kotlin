annotation class iHello
val v = 1

fun foo([i<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: iHello
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
