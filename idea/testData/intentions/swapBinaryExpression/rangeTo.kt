// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main() {
    for (i in 1<caret>..10) {
        doSomething("Hello World")
    }
}
