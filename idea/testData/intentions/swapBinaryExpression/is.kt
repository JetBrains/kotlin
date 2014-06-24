// IS_APPLICABLE: false
// ERROR: Expression 'if "test" is String' of type 'kotlin.Unit' cannot be invoked as a function. The function invoke() is not found

fun doSomething<T>(a: T) {}

fun main() {
    if <caret>"test" is String {
        doSomething("Hello")
    }
}
