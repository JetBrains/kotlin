// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun main() {
    for (elt<caret> in 0..3) {
        doSomething("Hello")
    }
}
