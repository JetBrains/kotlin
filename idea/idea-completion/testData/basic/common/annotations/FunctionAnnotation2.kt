// FIR_COMPARISON
annotation class Hello
val v = 1

@[<caret>] fun some() {}

// INVOCATION_COUNT: 0
// EXIST: Hello
// EXIST: Suppress
// ABSENT: String
// ABSENT: v
