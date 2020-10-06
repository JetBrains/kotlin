// PROBLEM: none
// WITH_RUNTIME

suspend fun mapString(input: String): String = ""

suspend fun main() {
    val sb = StringBuilder()
    val x = listOf("1", "2", "3").<caret>map { mapString(it) }.joinTo(sb)
}