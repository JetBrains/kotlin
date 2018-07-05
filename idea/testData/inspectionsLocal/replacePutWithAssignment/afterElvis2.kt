// PROBLEM: none
// WITH_RUNTIME

fun test(i: Int?, m: MutableMap<String, Int>) {
    i ?: ((m.<caret>put("", 1)))
}