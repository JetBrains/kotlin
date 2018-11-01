// PROBLEM: none
// WITH_RUNTIME
fun test(x: Any) {
    val s = run {
        <caret>x as? String ?: return
    }
}