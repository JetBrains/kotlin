// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(x: Int) {
    if (x !in 5..6) {
        doSomething("test")
    }
}
