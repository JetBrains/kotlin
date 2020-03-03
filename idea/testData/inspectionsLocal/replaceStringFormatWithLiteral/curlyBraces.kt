// WITH_RUNTIME
fun main() {
    val id = "abc"
    val date = "123"
    val s = String.format(<caret>"%s_%s_%s", id, date, id)
}