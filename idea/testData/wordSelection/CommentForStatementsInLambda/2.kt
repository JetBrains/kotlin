fun run(f: () -> Unit) = 1
fun test() {
    run {
        // test
<selection>        <caret>1
</selection>    }
}