// WITH_RUNTIME
fun test(x: Any) {
    run {
        <caret>x as? String ?: return
    }
}