// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun test(): Int {
    val sets = (1..100).associateWith { (it..10 * it).mapTo(java.util.TreeSet()) { i -> i } }
    val set = sets[50] ?: emptySet()
    return set.size
}