// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface I

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: I

value object O: IC() {
    val ok = "OK"
}

fun ic(): IC = O
fun icn(): IC? = O
fun icnn(): IC? = null
fun any(): Any = O
fun anyN(): Any? = O
fun anyNN(): Any? = null
fun c(): O = O
fun cn(): O? = O
fun cnn(): O? = null
fun i(): I = O
fun iN(): I? = O
fun inn(): I? = null

fun box(): String {
    var res = "FAIL 1"
    res = (ic() as O).ok
    if (res != "OK") return res
    res = (ic() as? O)?.ok ?: "FAIL 11"
    if (res != "OK") return res
    res = (ic() as? O)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") return res
    res = (ic() as? O)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") return res
    res = (ic() as? O)?.let { it.ok } ?: "FAIL 41"
    if (res != "OK") return res
    res = (ic() as? O)?.run { ok } ?: "FAIL 51"
    if (res != "OK") return res

    res = "FAIL 2"
    res = (icn() as O).ok
    if (res != "OK") return res
    res = (icn() as? O)?.ok ?: "FAIL 12"
    if (res != "OK") return res
    res = (icn() as? O)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res
    res = (icn() as? O)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") return res
    res = (icn() as? O)?.let { it.ok } ?: "FAIL 42"
    if (res != "OK") return res
    res = (icn() as? O)?.run { ok } ?: "FAIL 52"
    if (res != "OK") return res

    res = "FAIL 4"
    res = (any() as O).ok
    if (res != "OK") return res
    res = (any() as? O)?.ok ?: "FAIL 14"
    if (res != "OK") return res
    res = (any() as? O)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") return res
    res = (any() as? O)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") return res
    res = (any() as? O)?.let { it.ok } ?: "FAIL 44"
    if (res != "OK") return res
    res = (any() as? O)?.run { ok } ?: "FAIL 54"
    if (res != "OK") return res

    res = "FAIL 5"
    res = (anyN() as O).ok
    if (res != "OK") return res
    res = (anyN() as? O)?.ok ?: "FAIL 15"
    if (res != "OK") return res
    res = (anyN() as? O)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res
    res = (anyN() as? O)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") return res
    res = (anyN() as? O)?.let { it.ok } ?: "FAIL 45"
    if (res != "OK") return res
    res = (anyN() as? O)?.run { ok } ?: "FAIL 55"
    if (res != "OK") return res

    res = "FAIL 6"
    res = (icnn() as? O)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") return res
    res = (icnn() as? O)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") return res

    res = "FAIL 7"
    res = (anyNN() as? O)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") return res
    res = (anyNN() as? O)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") return res

    res = "FAIL 8"
    res = (c() as O).ok
    if (res != "OK") return res
    res = (c() as? O)?.ok ?: "FAIL 18"
    if (res != "OK") return res
    res = (c() as? O)?.let { "OK" } ?: "FAIL 28"
    if (res != "OK") return res
    res = (c() as? O)?.run { "OK" } ?: "FAIL 38"
    if (res != "OK") return res
    res = (c() as? O)?.let { it.ok } ?: "FAIL 48"
    if (res != "OK") return res
    res = (c() as? O)?.run { ok } ?: "FAIL 58"
    if (res != "OK") return res

    res = "FAIL 9"
    res = (cn() as O).ok
    if (res != "OK") return res
    res = (cn() as? O)?.ok ?: "FAIL 19"
    if (res != "OK") return res
    res = (cn() as? O)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") return res
    res = (cn() as? O)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") return res
    res = (cn() as? O)?.let { it.ok } ?: "FAIL 49"
    if (res != "OK") return res
    res = (cn() as? O)?.run { ok } ?: "FAIL 59"
    if (res != "OK") return res

    res = "FAIL 0"
    res = (cnn() as? O)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") return res
    res = (cnn() as? O)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") return res

    res = "FAIL A"
    res = (i() as O).ok
    if (res != "OK") return res
    res = (i() as? O)?.ok ?: "FAIL 1A"
    if (res != "OK") return res
    res = (i() as? O)?.let { "OK" } ?: "FAIL 2A"
    if (res != "OK") return res
    res = (i() as? O)?.run { "OK" } ?: "FAIL 3A"
    if (res != "OK") return res
    res = (i() as? O)?.let { it.ok } ?: "FAIL 4A"
    if (res != "OK") return res
    res = (i() as? O)?.run { ok } ?: "FAIL 5A"
    if (res != "OK") return res

    res = "FAIL B"
    res = (iN() as O).ok
    if (res != "OK") return res
    res = (iN() as? O)?.ok ?: "FAIL 1B"
    if (res != "OK") return res
    res = (iN() as? O)?.let { "OK" } ?: "FAIL 2B"
    if (res != "OK") return res
    res = (iN() as? O)?.run { "OK" } ?: "FAIL 3B"
    if (res != "OK") return res
    res = (iN() as? O)?.let { it.ok } ?: "FAIL 4B"
    if (res != "OK") return res
    res = (iN() as? O)?.run { ok } ?: "FAIL 5B"
    if (res != "OK") return res

    res = "FAIL C"
    res = (inn() as? O)?.let { "FAIL 1C" } ?: "OK"
    if (res != "OK") return res
    res = (inn() as? O)?.run { "FAIL 2C" } ?: "OK"
    if (res != "OK") return res

    return "OK"
}