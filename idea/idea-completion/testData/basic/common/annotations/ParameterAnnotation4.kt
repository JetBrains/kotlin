annotation class Hello
val v = 1

fun foo(volatile <caret>) { }

// INVOCATION_COUNT: 1
// EXIST: Hello
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
