// TARGET_BACKEND: JVM_IR, WASM

enum class E { A, B, C }

annotation class C(
    val i: Int = 1,
    val s: String = "",
    val a: IntArray = intArrayOf(),
    val e: E = E.A
)

fun box(): String {
    val a = C()
    val b = C(i = 1, s = "", a = intArrayOf(), e = E.A)
    if (a != b) return "Fail1"
    if (a.hashCode() != b.hashCode()) return "Fail2"
    return "OK"
}
