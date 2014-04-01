// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main() {
    val c = 500
    doSomething(<caret>"Today is Friday")
}
