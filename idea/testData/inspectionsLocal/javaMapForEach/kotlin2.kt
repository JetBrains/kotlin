// PROBLEM: none
// WITH_RUNTIME
fun test(map: Map<Int, String>) {
    map.<caret>forEach {
        foo(it)
    }
}
fun foo(it: Map.Entry<Int, String>) {}