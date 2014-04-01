// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main(x: Int) {
    if (x !in 5..6) {
        doSomething("test")
    }
}
