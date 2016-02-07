// "Create property 'foo'" "true"
// ERROR: Property must be initialized

fun test() {
    val x: Int by <caret>foo
}
