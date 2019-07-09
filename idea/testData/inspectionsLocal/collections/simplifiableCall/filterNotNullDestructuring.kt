// WITH_RUNTIME
// PROBLEM: none
fun test(map: Map<String?, String?>) {
    map.<caret>filter { (k, v) -> k != null && v != null }
}