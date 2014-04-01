// IS_APPLICABLE: false

fun doSomething<T>(a: T) {}

fun main() {
    if <caret>"test" is String {
        doSomething("Hello")
    }
}
