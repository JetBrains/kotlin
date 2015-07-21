// WITH_RUNTIME
fun foo(bar: Sequence<String>) {
    for (<caret>a in bar)
        print(a)
}