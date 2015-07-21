// WITH_RUNTIME
fun foo(bar: String) {
    for (<caret>a in bar) {
        print(a)
    }
}