// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun foo() {
    val a = true
    val b = false
    when (<caret>a && b) {
        else -> doSomething("test")
    }
}
