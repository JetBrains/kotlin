// "Lift assignment out of 'try' expression" "false"
// ACTION: Make variable mutable
// ACTION: Remove redundant assignment
// ERROR: Val cannot be reassigned
// WITH_RUNTIME

fun foo(arg: Boolean) {
    val x: Int
    try {
        if (arg) {
            x = 1
        }
    }
    catch (e: Exception) {
        <caret>x = 2
    }
}