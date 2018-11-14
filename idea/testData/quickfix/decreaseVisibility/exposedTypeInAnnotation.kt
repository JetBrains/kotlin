// "Make '<init>' internal" "false"
// DISABLE-ERRORS
// ACTION: Make 'My' public

internal class My

annotation class Your(val x: <caret>My)