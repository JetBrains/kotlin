// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main() {
    for (i in 1<caret>..10) {
        doSomething("Hello World")
    }
}
