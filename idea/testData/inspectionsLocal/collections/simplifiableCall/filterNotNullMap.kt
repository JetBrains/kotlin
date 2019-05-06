// WITH_RUNTIME
// PROBLEM: none
fun test(map: Map<String?, String?>) {
    map.<caret>filter { it != null }
}