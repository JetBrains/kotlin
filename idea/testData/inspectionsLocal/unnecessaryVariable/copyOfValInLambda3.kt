// WITH_RUNTIME
fun findCredentials() = 1

fun println(i: Int) {}

fun test() {
    val credentials = findCredentials()

    val data = run {
        object {
            val <caret>foundCredentials = credentials
        }
    }

    println(data.foundCredentials)
}
