// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface I

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: I

value class C(val ok: String): IC()

fun ic(a: IC) {
    var res = "FAIL 1"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 11"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 41"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 51"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 61")
}

fun icn(a: IC?) {
    var res = "FAIL 2"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 12"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 42"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 52"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 62")
}

fun icnn(a: IC?) {
    var res = "FAIL 6"
    res = (a as? C)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") error(res)
    if (a is C) error("FAIL 36")
}

fun any(a: Any) {
    var res = "FAIL 4"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 14"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 44"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 54"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 64")
}

fun anyN(a: Any?) {
    var res = "FAIL 5"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 15"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 45"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 55"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 65")
}

fun anyNN(a: Any?) {
    var res = "FAIL 7"
    res = (a as? C)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") error(res)
    if (a is C) error("FAIL 37")
}

fun c(a: C) {
    var res = "FAIL 8"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 18"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 28"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 38"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 48"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 58"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 68")
}

fun cn(a: C?) {
    var res = "FAIL 9"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 19"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 49"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 59"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 69")
}

fun cnn(a: C?) {
    var res = "FAIL 0"
    res = (a as? C)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") error(res)
    if (a is C) error("FAIL 30")
}

fun i(a: I) {
    var res = "FAIL A"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 1A"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 2A"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 3A"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 2A"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 3A"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 3A")
}

fun iN(a: I?) {
    var res = "FAIL B"
    res = (a as C).ok
    if (res != "OK") error(res)
    res = (a as? C)?.ok ?: "FAIL 1B"
    if (res != "OK") error(res)
    res = (a as? C)?.let { "OK" } ?: "FAIL 2B"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "OK" } ?: "FAIL 3B"
    if (res != "OK") error(res)
    res = (a as? C)?.let { it.ok } ?: "FAIL 2B"
    if (res != "OK") error(res)
    res = (a as? C)?.run { ok } ?: "FAIL 3B"
    if (res != "OK") error(res)
    if (a !is C) error("FAIL 3B")
}

fun iNN(a: I?) {
    var res = "FAIL C"
    res = (a as? C)?.let { "FAIL 1C" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? C)?.run { "FAIL 2C" } ?: "OK"
    if (res != "OK") error(res)
    if (a is C) error("FAIL 3C")
}

fun box(): String {
    ic(C("OK"))
    icn(C("OK"))
    icnn(null)
    any(C("OK"))
    anyN(C("OK"))
    anyNN(null)
    c(C("OK"))
    cn(C("OK"))
    cnn(null)
    i(C("OK"))
    iN(C("OK"))
    iNN(null)

    return "OK"
}