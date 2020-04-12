// PROBLEM: none
// DISABLE-ERRORS
expect suspend fun a()

actual <caret>suspend fun a() {
}