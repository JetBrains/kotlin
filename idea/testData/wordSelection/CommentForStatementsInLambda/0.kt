fun run(f: () -> Unit) = 1
fun test() {
    run {
        // test
        <caret>1
    }
}