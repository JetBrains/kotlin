// "Create extension property 'bar'" "true"
// ERROR: Property must be initialized
fun consume(n: Int) {}

fun <T> foo(t: T) {
    consume(t.<caret>bar)
}