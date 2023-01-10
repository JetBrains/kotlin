// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface I

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: I

value class C(val ok: String): IC()

fun ic(): IC = C("OK")
fun icn(): IC? = C("OK")
fun icnn(): IC? = null
fun any(): Any = C("OK")
fun anyN(): Any? = C("OK")
fun anyNN(): Any? = null
fun c(): C = C("OK")
fun cn(): C? = C("OK")
fun cnn(): C? = null
fun i(): I = C("OK")
fun iN(): I? = C("OK")
fun inn(): I? = null

fun box(): String {
    var res = "FAIL 1"
    res = (ic() as C).ok
    if (res != "OK") return res
    res = (ic() as? C)?.ok ?: "FAIL 11"
    if (res != "OK") return res
    res = (ic() as? C)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") return res
    res = (ic() as? C)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") return res
    res = (ic() as? C)?.let { it.ok } ?: "FAIL 41"
    if (res != "OK") return res
    res = (ic() as? C)?.run { ok } ?: "FAIL 51"
    if (res != "OK") return res
    if (ic() !is C) return "FAIL 61"

    res = "FAIL 2"
    res = (icn() as C).ok
    if (res != "OK") return res
    res = (icn() as? C)?.ok ?: "FAIL 12"
    if (res != "OK") return res
    res = (icn() as? C)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res
    res = (icn() as? C)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") return res
    res = (icn() as? C)?.let { it.ok } ?: "FAIL 42"
    if (res != "OK") return res
    res = (icn() as? C)?.run { ok } ?: "FAIL 52"
    if (res != "OK") return res
    if (icn() !is C) return "FAIL 62"

    res = "FAIL 4"
    res = (any() as C).ok
    if (res != "OK") return res
    res = (any() as? C)?.ok ?: "FAIL 14"
    if (res != "OK") return res
    res = (any() as? C)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") return res
    res = (any() as? C)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") return res
    res = (any() as? C)?.let { it.ok } ?: "FAIL 44"
    if (res != "OK") return res
    res = (any() as? C)?.run { ok } ?: "FAIL 54"
    if (res != "OK") return res
    if (any() !is C) return "FAIL 64"

    res = "FAIL 5"
    res = (anyN() as C).ok
    if (res != "OK") return res
    res = (anyN() as? C)?.ok ?: "FAIL 15"
    if (res != "OK") return res
    res = (anyN() as? C)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res
    res = (anyN() as? C)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") return res
    res = (anyN() as? C)?.let { it.ok } ?: "FAIL 45"
    if (res != "OK") return res
    res = (anyN() as? C)?.run { ok } ?: "FAIL 55"
    if (res != "OK") return res
    if (anyN() !is C) return "FAIL 65"

    res = "FAIL 6"
    res = (icnn() as? C)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") return res
    res = (icnn() as? C)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") return res
    if (icnn() is C) return "FAIL 36"

    res = "FAIL 7"
    res = (anyNN() as? C)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") return res
    res = (anyNN() as? C)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") return res
    if (anyNN() is C) return "FAIL 37"

    res = "FAIL 8"
    res = (c() as C).ok
    if (res != "OK") return res
    res = (c() as? C)?.ok ?: "FAIL 18"
    if (res != "OK") return res
    res = (c() as? C)?.let { "OK" } ?: "FAIL 28"
    if (res != "OK") return res
    res = (c() as? C)?.run { "OK" } ?: "FAIL 38"
    if (res != "OK") return res
    res = (c() as? C)?.let { it.ok } ?: "FAIL 48"
    if (res != "OK") return res
    res = (c() as? C)?.run { ok } ?: "FAIL 58"
    if (res != "OK") return res
    if (c() !is C) return "FAIL 68"

    res = "FAIL 9"
    res = (cn() as C).ok
    if (res != "OK") return res
    res = (cn() as? C)?.ok ?: "FAIL 19"
    if (res != "OK") return res
    res = (cn() as? C)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") return res
    res = (cn() as? C)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") return res
    res = (cn() as? C)?.let { it.ok } ?: "FAIL 49"
    if (res != "OK") return res
    res = (cn() as? C)?.run { ok } ?: "FAIL 59"
    if (res != "OK") return res
    if (cn() !is C) return "FAIL 69"

    res = "FAIL 0"
    res = (cnn() as? C)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") return res
    res = (cnn() as? C)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") return res
    if (cnn() is C) return "FAIL 30"

    res = "FAIL A"
    res = (i() as C).ok
    if (res != "OK") return res
    res = (i() as? C)?.ok ?: "FAIL 1A"
    if (res != "OK") return res
    res = (i() as? C)?.let { "OK" } ?: "FAIL 2A"
    if (res != "OK") return res
    res = (i() as? C)?.run { "OK" } ?: "FAIL 3A"
    if (res != "OK") return res
    res = (i() as? C)?.let { it.ok } ?: "FAIL 4A"
    if (res != "OK") return res
    res = (i() as? C)?.run { ok } ?: "FAIL 5A"
    if (res != "OK") return res
    if (i() !is C) return "FAIL 6A"

    res = "FAIL B"
    res = (iN() as C).ok
    if (res != "OK") return res
    res = (iN() as? C)?.ok ?: "FAIL 1B"
    if (res != "OK") return res
    res = (iN() as? C)?.let { "OK" } ?: "FAIL 2B"
    if (res != "OK") return res
    res = (iN() as? C)?.run { "OK" } ?: "FAIL 3B"
    if (res != "OK") return res
    res = (iN() as? C)?.let { it.ok } ?: "FAIL 4B"
    if (res != "OK") return res
    res = (iN() as? C)?.run { ok } ?: "FAIL 5B"
    if (res != "OK") return res
    if (iN() !is C) return "FAIL 6B"

    res = "FAIL C"
    res = (inn() as? C)?.let { "FAIL 1C" } ?: "OK"
    if (res != "OK") return res
    res = (inn() as? C)?.run { "FAIL 2C" } ?: "OK"
    if (res != "OK") return res
    if (inn() is C) return "FAIL 3B"

    return "OK"
}