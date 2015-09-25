annotation class SHello
val v = 1

fun foo(@[S<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: SHello
// EXIST: Suppress
// ABSENT: String
// ABSENT: v
