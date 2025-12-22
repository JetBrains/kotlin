// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-83349 Wrong hashCode values in instantiated annotations

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
