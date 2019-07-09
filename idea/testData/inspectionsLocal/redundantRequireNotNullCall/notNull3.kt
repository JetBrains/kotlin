// WITH_RUNTIME
fun test(s: String?) {
    requireNotNull(s)
    <caret>requireNotNull(s)
    println(1)
}