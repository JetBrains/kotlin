// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

value object O: IC() {
    val ok = "OK"
}

fun box(): String {
    var res = "FAIL 1"
    val ic: IC = O
    res = (ic as O).ok
    if (res != "OK") return res
    res = (ic as? O)?.ok ?: "FAIL 11"
    if (res != "OK") return res
    res = (ic as? O)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") return res
    res = (ic as? O)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") return res
    res = (ic as? O)?.let { it.ok } ?: "FAIL 41"
    if (res != "OK") return res
    res = (ic as? O)?.run { ok } ?: "FAIL 51"
    if (res != "OK") return res
    if (ic !is O) return "FAIL 61"

    res = "FAIL 2"
    val icn: IC? = ic
    res = (icn as O).ok
    if (res != "OK") return res
    res = (icn as? O)?.ok ?: "FAIL 12"
    if (res != "OK") return res
    res = (icn as? O)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res
    res = (icn as? O)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") return res
    res = (icn as? O)?.let { it.ok } ?: "FAIL 42"
    if (res != "OK") return res
    res = (icn as? O)?.run { ok } ?: "FAIL 52"
    if (res != "OK") return res
    if (icn !is O) return "FAIL 62"

    res = "FAIL 3"
    val ic2 = (icn as IC)
    res = (ic2 as O).ok
    if (res != "OK") return res
    res = (ic2 as? O)?.ok ?: "FAIL 13"
    if (res != "OK") return res
    res = (ic2 as? O)?.let { "OK" } ?: "FAIL 23"
    if (res != "OK") return res
    res = (ic2 as? O)?.run { "OK" } ?: "FAIL 33"
    if (res != "OK") return res
    res = (ic2 as? O)?.let { it.ok } ?: "FAIL 43"
    if (res != "OK") return res
    res = (ic2 as? O)?.run { ok } ?: "FAIL 53"
    if (res != "OK") return res
    if (ic2 !is O) return "FAIL 63"

    res = "FAIL 4"
    val any = (icn as Any)
    res = (any as O).ok
    if (res != "OK") return res
    res = (any as? O)?.ok ?: "FAIL 14"
    if (res != "OK") return res
    res = (any as? O)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") return res
    res = (any as? O)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") return res
    res = (any as? O)?.let { it.ok } ?: "FAIL 44"
    if (res != "OK") return res
    res = (any as? O)?.run { ok } ?: "FAIL 54"
    if (res != "OK") return res
    if (any !is O) return "FAIL 64"

    res = "FAIL 5"
    val anyN = (icn as Any?)
    res = (anyN as O).ok
    if (res != "OK") return res
    res = (anyN as? O)?.ok ?: "FAIL 15"
    if (res != "OK") return res
    res = (anyN as? O)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res
    res = (anyN as? O)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") return res
    res = (anyN as? O)?.let { it.ok } ?: "FAIL 45"
    if (res != "OK") return res
    res = (anyN as? O)?.run { ok } ?: "FAIL 55"
    if (res != "OK") return res
    if (anyN !is O) return "FAIL 65"

    res = "FAIL 6"
    val icnn: IC? = null
    res = (icnn as? O)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") return res
    res = (icnn as? O)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") return res
    if (icnn is O) return "FAIL 36"

    res = "FAIL 7"
    val anyNN: Any? = null
    res = (anyNN as? O)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") return res
    res = (anyNN as? O)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") return res
    if (anyNN is O) return "FAIL 37"

    return "OK"
}