annotation class Hello
val v = 1

<caret>
class C

// INVOCATION_COUNT: 0
// EXIST: Hello
// EXIST: inlineOptions
// ABSENT: String
// ABSENT: v
