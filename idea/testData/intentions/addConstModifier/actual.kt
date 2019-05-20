// IS_APPLICABLE: false
// DISABLE-ERRORS

expect object C {
    val name: String
}

actual object C {
    actual val <caret>name: String = ""
}