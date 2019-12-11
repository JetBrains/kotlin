// !LANGUAGE: +NewInference

fun <K> id1(k: K): K = k
fun <V> id2(v: V): V = v

fun test() {
    id1 {
        id2 {
            3
        }
    }
}
