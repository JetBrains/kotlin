// "Lift assignment out of 'try' expression" "false"
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