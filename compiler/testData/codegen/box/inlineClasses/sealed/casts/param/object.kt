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

fun ic(a: IC) {
    var res = "FAIL 1"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 11"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 21"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 31"
    if (res != "OK") error(res)
}

fun icn(a: IC?) {
    var res = "FAIL 2"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 12"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 22"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 32"
    if (res != "OK") error(res)
}

fun icnn(a: IC?) {
    var res = "FAIL 6"
    res = (a as? O)?.let { "FAIL 16" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "FAIL 26" } ?: "OK"
    if (res != "OK") error(res)
}

fun any(a: Any) {
    var res = "FAIL 4"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 14"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 24"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 34"
    if (res != "OK") error(res)
}

fun anyN(a: Any?) {
    var res = "FAIL 5"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 15"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 25"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 35"
    if (res != "OK") error(res)
}

fun anyNN(a: Any?) {
    var res = "FAIL 7"
    res = (a as? O)?.let { "FAIL 17" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "FAIL 27" } ?: "OK"
    if (res != "OK") error(res)
}

fun c(a: O) {
    var res = "FAIL 8"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 18"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 28"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 38"
    if (res != "OK") error(res)
}

fun cn(a: O?) {
    var res = "FAIL 9"
    res = (a as O).ok
    if (res != "OK") error(res)
    res = (a as? O)?.ok ?: "FAIL 19"
    if (res != "OK") error(res)
    res = (a as? O)?.let { "OK" } ?: "FAIL 29"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "OK" } ?: "FAIL 39"
    if (res != "OK") error(res)
}

fun cnn(a: O?) {
    var res = "FAIL 0"
    res = (a as? O)?.let { "FAIL 10" } ?: "OK"
    if (res != "OK") error(res)
    res = (a as? O)?.run { "FAIL 20" } ?: "OK"
    if (res != "OK") error(res)
}

fun box(): String {
    ic(O)
    icn(O)
    icnn(null)
    any(O)
    anyN(O)
    anyNN(null)
    c(O)
    cn(O)
    cnn(null)

    return "OK"
}