// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    val a = true
    val b = false
    when (<caret>a && b) {
        else -> doSomething("test")
    }
}
