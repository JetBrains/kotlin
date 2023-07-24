// FILE: main.kt
class A(override val <caret>p: String): B(p)

// FILE: B.kt
open class B(val p: String)


// RESULT
// ALL:
// B.p: String

// DIRECT:
// B.p: String
