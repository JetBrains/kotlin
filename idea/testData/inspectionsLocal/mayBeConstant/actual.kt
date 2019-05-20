// PROBLEM: none
// DISABLE-ERRORS

expect object C {
    val name: String
}

actual object C {
    actual val <caret>name: String = ""
}