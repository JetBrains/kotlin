// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun <K> id1(k: K): K = k
fun <V> id2(v: V): V = v

fun test() {
    id1 {
        id2 {
            3
        }
    }
}
