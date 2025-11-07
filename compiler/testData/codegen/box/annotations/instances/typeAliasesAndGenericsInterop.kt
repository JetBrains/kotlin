// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

typealias Str = String
typealias StrArr = Array<Str>

annotation class AnnAlias(val s: StrArr, val k: kotlin.reflect.KClass<*> = Any::class)

fun box(): String {
    val a1 = AnnAlias(arrayOf("a", "b"), String::class)
    val a2 = AnnAlias(arrayOf("a", "b"), String::class)
    val a3 = AnnAlias(arrayOf("a", "b"), Int::class)

    if (a1 != a2) return "Fail1"
    if (a1.hashCode() != a2.hashCode()) return "Fail2"
    if (a1 == a3) return "Fail3"

    val ts = a1.toString()
    if (ts.isEmpty() || !ts.contains("AnnAlias(")) return "Fail4"

    return "OK"
}
