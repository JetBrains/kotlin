// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

enum class E { A, B }

annotation class AClass(val k: kotlin.reflect.KClass<*>, val e: E)

fun box(): String {
    val a1 = AClass(String::class, E.B)
    val a2 = AClass(String::class, E.B)
    val a3 = AClass(Int::class, E.B)
    val a4 = AClass(String::class, E.A)

    if (a1 != a2) return "Fail1"
    if (a1.hashCode() != a2.hashCode()) return "Fail2"
    if (a1 == a3) return "Fail3"
    if (a1 == a4) return "Fail4"

    val ts = a1.toString()
    if (ts.isEmpty() || !ts.contains("AClass(")) return "Fail5"

    return "OK"
}