// WITH_RUNTIME
fun test(i: Int) {
    println(<caret>requireNotNull(i) { "" })
}