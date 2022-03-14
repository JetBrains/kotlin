// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface I

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: I

value class C(val ok: String): IC()

value object O: IC() {
    val ok: String = "OK"
}

fun box(): String {
    val ic: IC = C("OK")
    val io: IC = O

    var res = "FAIL 1"
    val iC: I = ic
    res = (iC as C).ok
    if (res != "OK") return res
    res = (iC as? C)?.ok ?: "FAIL 11"
    if (res != "OK") return res
    res = (iC as? C)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") return res
    res = (iC as? C)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") return res

    res = "FAIL 2"
    val iO: I = io
    res = (iO as O).ok
    if (res != "OK") return res
    res = (iO as? O)?.ok ?: "FAIL 12"
    if (res != "OK") return res
    res = (iO as? O)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res
    res = (iO as? O)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") return res

    res = "FAIL 3"
    val iCN: I? = ic
    res = (iCN as C).ok
    if (res != "OK") return res
    res = (iCN as? C)?.ok ?: "FAIL 13"
    if (res != "OK") return res
    res = (iCN as? C)?.let { "OK" } ?: "FAIL 23"
    if (res != "OK") return res
    res = (iCN as? C)?.run { "OK" } ?: "FAIL 33"
    if (res != "OK") return res

    res = "FAIL 4"
    val iON: I? = io
    res = (iON as O).ok
    if (res != "OK") return res
    res = (iON as? O)?.ok ?: "FAIL 14"
    if (res != "OK") return res
    res = (iON as? O)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") return res
    res = (iON as? O)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") return res

    val iNull: I? = null
    res = (iNull as? C)?.let { "FAIL 5" } ?: "OK"
    if (res != "OK") return res
    res = (iNull as? O)?.run { "FAIL 6" } ?: "OK"
    if (res != "OK") return res

    return "OK"
}