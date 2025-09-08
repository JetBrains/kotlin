// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

enum class E { A, B, C }

annotation class WithEnum(
    val e: E,
    val es: Array<E>
)

fun box(): String {
    val x = WithEnum(E.B, arrayOf(E.A, E.C))
    val y = WithEnum(E.B, arrayOf(E.A, E.C))
    val z = WithEnum(E.B, arrayOf(E.C, E.A))

    if (x != y) return "Fail1"
    if (x == z) return "Fail2"
    if (x.hashCode() != y.hashCode()) return "Fail3"

    return "OK"
}