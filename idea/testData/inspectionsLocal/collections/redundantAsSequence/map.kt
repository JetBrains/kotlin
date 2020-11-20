// PROBLEM: none
// WITH_RUNTIME
fun test(map: Map<String, Int>): Map.Entry<String, Int>? {
    return map.<caret>asSequence().firstOrNull()
}