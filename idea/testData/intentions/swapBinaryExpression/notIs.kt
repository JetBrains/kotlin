// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(x: Int) {
    if (x !is Int) {
        doSomething("test")
    }
}
