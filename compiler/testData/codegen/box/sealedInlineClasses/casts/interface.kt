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

OPTIONAL_JVM_INLINE_ANNOTATION
value class VC(val ok: String): IC()

fun box(): String {
    val ic: IC = C("OK")
    val io: IC = O
    val vc: IC = VC("OK")

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
    res = (iC as? C)?.let { it.ok } ?: "FAIL 41"
    if (res != "OK") return res
    res = (iC as? C)?.run { ok } ?: "FAIL 51"
    if (res != "OK") return res
    if (iC !is C) return "FAIL 61"
    if (iC is O) return "FAIL 71"
    if (iC is VC) return "FAIL 71"

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
    res = (iO as? O)?.let { it.ok } ?: "FAIL 42"
    if (res != "OK") return res
    res = (iO as? O)?.run { ok } ?: "FAIL 52"
    if (res != "OK") return res
    if (iO !is O) return "FAIL 62"
    if (iO is C) return "FAIL 72"
    if (iO is VC) return "FAIL 82"

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
    res = (iCN as? C)?.let { it.ok } ?: "FAIL 43"
    if (res != "OK") return res
    res = (iCN as? C)?.run { ok } ?: "FAIL 53"
    if (res != "OK") return res
    if (iCN !is C) return "FAIL 63"
    if (iCN is O) return "FAIL 73"
    if (iCN is VC) return "FAIL 83"

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
    res = (iON as? O)?.let { it.ok } ?: "FAIL 24"
    if (res != "OK") return res
    res = (iON as? O)?.run { ok } ?: "FAIL 34"
    if (res != "OK") return res
    if (iON !is O) return "FAIL 64"
    if (iON is C) return "FAIL 74"
    if (iON is VC) return "FAIL 84"

    res = "FAIL 5"
    val vC: I = vc
    res = (vC as VC).ok
    if (res != "OK") return res
    res = (vC as? VC)?.ok ?: "FAIL 15"
    if (res != "OK") return res
    res = (vC as? VC)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res
    res = (vC as? VC)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") return res
    res = (vC as? VC)?.let { it.ok } ?: "FAIL 45"
    if (res != "OK") return res
    res = (vC as? VC)?.run { ok } ?: "FAIL 55"
    if (res != "OK") return res
    if (vC !is VC) return "FAIL 65"
    if (vC is C) return "FAIL 75"
    if (vC is O) return "FAIL 85"

    res = "FAIL 6"
    val vCN: I? = vc
    res = (vCN as VC).ok
    if (res != "OK") return res
    res = (vCN as? VC)?.ok ?: "FAIL 16"
    if (res != "OK") return res
    res = (vCN as? VC)?.let { "OK" } ?: "FAIL 26"
    if (res != "OK") return res
    res = (vCN as? VC)?.run { "OK" } ?: "FAIL 36"
    if (res != "OK") return res
    res = (vCN as? VC)?.let { it.ok } ?: "FAIL 46"
    if (res != "OK") return res
    res = (vCN as? VC)?.run { ok } ?: "FAIL 56"
    if (res != "OK") return res
    if (vCN !is VC) return "FAIL 66"
    if (vCN is C) return "FAIL 76"
    if (vCN is O) return "FAIL 86"

    val iNull: I? = null
    res = (iNull as? C)?.let { "FAIL 7" } ?: "OK"
    if (res != "OK") return res
    res = (iNull as? O)?.run { "FAIL 8" } ?: "OK"
    if (res != "OK") return res
    if (iNull is C) return "FAIL 90"
    if (iNull is O) return "FAIL 91"
    if (iNull is VC) return "FAIL 92"

    return "OK"
}