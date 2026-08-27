// ISSUE: KT-88860
fun <T : Any> take(value: T?): T = value!!

fun produce(): String = "x"

fun main() {
    val result = produce()
    if (result == null) {
        take(result)
    }
}
