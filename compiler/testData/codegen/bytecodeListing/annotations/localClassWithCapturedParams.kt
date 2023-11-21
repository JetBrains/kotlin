// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

annotation class Simple(val value: String)

fun localCaptured(): Any {
    val z  = 1
    class A(@Simple("K") val z: String) {
        val x = z
    }
    return A("K")
}
