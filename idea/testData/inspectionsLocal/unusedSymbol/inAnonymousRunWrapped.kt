// PROBLEM: none
// WITH_RUNTIME

fun testDC(): String {
    val x = run {
        object {
            val <caret>users = "XXX"
        }
    }

    return x.users
}