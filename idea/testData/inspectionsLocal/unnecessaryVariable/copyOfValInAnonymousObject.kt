// PROBLEM: none
// WITH_RUNTIME
fun findCredentials() = 1

fun println(i: Int) {}

fun test() {
    val data = run {
        val credentials = findCredentials()
        object {
            val <caret>foundCredentials = credentials
        }
    }

    println(data.foundCredentials)
}
