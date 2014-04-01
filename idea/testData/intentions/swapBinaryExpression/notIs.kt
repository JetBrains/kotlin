// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main(x: Int) {
    if (x !is Int) {
        doSomething("test")
    }
}
