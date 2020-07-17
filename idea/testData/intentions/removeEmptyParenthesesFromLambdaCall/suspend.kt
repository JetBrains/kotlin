// IS_APPLICABLE: false
fun suspend(body: () -> Int) {}

fun main() {
    val wInvokeCall = suspend()<caret> { 42 }
}