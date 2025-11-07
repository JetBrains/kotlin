// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

annotation class Inner(val v: String)

annotation class AnnArrays(
    val ints: IntArray = intArrayOf(),
    val strs: Array<String> = [],
    val inn: Inner = Inner("x")
)

fun box(): String {
    val a1 = AnnArrays(ints = intArrayOf(1, 2, 3), strs = arrayOf("a", "b"), inn = Inner("qq"))
    val a2 = AnnArrays(ints = intArrayOf(1, 2, 3), strs = arrayOf("a", "b"), inn = Inner("qq"))
    val a3 = AnnArrays(ints = intArrayOf(3, 2, 1), strs = arrayOf("a", "b"), inn = Inner("qq"))

    if (a1 != a2) return "Fail1"
    if (a1.hashCode() != a2.hashCode()) return "Fail2"
    if (a1 == a3) return "Fail3"

    val ts = a1.toString()
    if (ts.isEmpty() || !ts.contains("AnnArrays(")) return "Fail4"

    return "OK"
}
