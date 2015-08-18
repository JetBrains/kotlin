// "Create property 'foo'" "true"
// ERROR: Local variables are not allowed to have delegates
// ERROR: Property must be initialized

fun test() {
    val x: Int by <caret>foo
}
