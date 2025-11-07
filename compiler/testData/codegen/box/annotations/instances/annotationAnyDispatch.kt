// WITH_STDLIB
// TARGET_BACKEND: JVM_IR, WASM

annotation class Ann(val x: Int = 1, val s: String = "k")

fun box(): String {
    val a1 = Ann(7, "ok")
    val a2 = Ann(7, "ok")
    val a3 = Ann(8, "ok")

    val any1: Any = a1
    val any2: Any = a2
    val any3: Any = a3

    if (!any1.equals(any2)) return "Fail1"
    if (any1.hashCode() != any2.hashCode()) return "Fail2"
    if (any1.equals(any3)) return "Fail3"

    val eq: (Any, Any?) -> Boolean = Any::equals
    val hc: (Any) -> Int = Any::hashCode
    val ts: (Any) -> String = Any::toString

    if (!eq(any1, any2)) return "Fail4"
    if (hc(any1) != hc(any2)) return "Fail5"

    val t = ts(any1)
    if (t.isEmpty() || !t.startsWith("@") || !t.contains("Ann")) return "Fail6"

    val boundTs = any1::toString
    val bt = boundTs()
    if (bt.isEmpty() || !bt.contains("Ann")) return "Fail7"

    return "OK"
}
