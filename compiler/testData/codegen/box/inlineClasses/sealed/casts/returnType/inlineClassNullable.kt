// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val ok: String?): IC()

fun ic(): IC = C("OK")
fun icn(): IC? = C("OK")
fun icnn(): IC? = null
fun any(): Any = C("OK")
fun anyN(): Any? = C("OK")
fun anyNN(): Any? = null
fun c(): C = C("OK")
fun cn(): C? = C("OK")
fun cnn(): C? = null

fun box(): String {
    var res: String? = "FAIL 1"
    res = (ic() as C).ok
    if (res != "OK") return res!!
    res = (ic() as? C)?.ok ?: "FAIL 11"
    if (res != "OK") return res!!
    res = (ic() as? C)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") return res!!
    res = (ic() as? C)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") return res!!

    res = "FAIL 2"
    res = (icn() as C).ok
    if (res != "OK") return res!!
    res = (icn() as? C)?.ok ?: "FAIL 12"
    if (res != "OK") return res!!
    res = (icn() as? C)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") return res!!
    res = (icn() as? C)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") return res!!

    res = "FAIL 4"
    res = (any() as C).ok
    if (res != "OK") return res!!
    res = (any() as? C)?.ok ?: "FAIL 14"
    if (res != "OK") return res!!
    res = (any() as? C)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") return res!!
    res = (any() as? C)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") return res!!

    res = "FAIL 5"
    res = (anyN() as C).ok
    if (res != "OK") return res!!
    res = (anyN() as? C)?.ok ?: "FAIL 15"
    if (res != "OK") return res!!
    res = (anyN() as? C)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") return res!!
    res = (anyN() as? C)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") return res!!

    res = "FAIL 6"
    res = (icnn() as? C)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") return res!!
    res = (icnn() as? C)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") return res!!

    res = "FAIL 7"
    res = (anyNN() as? C)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") return res!!
    res = (anyNN() as? C)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") return res!!

    res = "FAIL 8"
    res = (c() as C).ok
    if (res != "OK") return res!!
    res = (c() as? C)?.ok ?: "FAIL 18"
    if (res != "OK") return res!!
    res = (c() as? C)?.let { "OK" } ?: "FAIL 28"
    if (res != "OK") return res!!
    res = (c() as? C)?.run { "OK" } ?: "FAIL 38"
    if (res != "OK") return res!!

    res = "FAIL 9"
    res = (cn() as C).ok
    if (res != "OK") return res!!
    res = (cn() as? C)?.ok ?: "FAIL 19"
    if (res != "OK") return res!!
    res = (cn() as? C)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") return res!!
    res = (cn() as? C)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") return res!!

    res = "FAIL 0"
    res = (cnn() as? C)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") return res!!
    res = (cnn() as? C)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") return res!!

    return "OK"
}