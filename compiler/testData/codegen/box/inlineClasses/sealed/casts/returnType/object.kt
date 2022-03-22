// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

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

    res = "FAIL 2"
    res = (icn() as O).ok
    if (res != "OK") return res
    res = (icn() as? O)?.ok ?: "FAIL 12"
    if (res != "OK") return res
    res = (icn() as? O)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res
    res = (icn() as? O)?.run { "OK" } ?: "FAIL 32"
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

    res = "FAIL 5"
    res = (anyN() as O).ok
    if (res != "OK") return res
    res = (anyN() as? O)?.ok ?: "FAIL 15"
    if (res != "OK") return res
    res = (anyN() as? O)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res
    res = (anyN() as? O)?.run { "OK" } ?: "FAIL 35"
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

    res = "FAIL 9"
    res = (cn() as O).ok
    if (res != "OK") return res
    res = (cn() as? O)?.ok ?: "FAIL 19"
    if (res != "OK") return res
    res = (cn() as? O)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") return res
    res = (cn() as? O)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") return res

    res = "FAIL 0"
    res = (cnn() as? O)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") return res
    res = (cnn() as? O)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") return res

    return "OK"
}