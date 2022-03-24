// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val ok: Int): IC()

fun ic(a: IC) {
    var res = 1
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 11
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 21
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 31
    if (res != 100) error("$res")
}

fun icn(a: IC?) {
    var res = 2
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 12
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 22
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 32
    if (res != 100) error("$res")
}

fun icnn(a: IC?) {
    var res = 6
    res = (a as? C)?.let { 16 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 26 } ?: 100
    if (res != 100) error("$res")
}

fun any(a: Any) {
    var res = 4
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 14
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 24
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 34
    if (res != 100) error("$res")
}

fun anyN(a: Any?) {
    var res = 5
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 15
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 25
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 35
    if (res != 100) error("$res")
}

fun anyNN(a: Any?) {
    var res = 7
    res = (a as? C)?.let { 17 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 27 } ?: 100
    if (res != 100) error("$res")
}

fun c(a: C) {
    var res = 8
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 18
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 28
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 38
    if (res != 100) error("$res")
}

fun cn(a: C?) {
    var res = 9
    res = (a as C).ok
    if (res != 100) error("$res")
    res = (a as? C)?.ok ?: 19
    if (res != 100) error("$res")
    res = (a as? C)?.let { 100 } ?: 29
    if (res != 100) error("$res")
    res = (a as? C)?.run { 100 } ?: 39
    if (res != 100) error("$res")
}

fun cnn(a: C?) {
    var res = 0
    res = (a as? C)?.let { 10 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 20 } ?: 100
    if (res != 100) error("$res")
}

fun box(): String {
    ic(C(100))
    icn(C(100))
    icnn(null)
    any(C(100))
    anyN(C(100))
    anyNN(null)
    c(C(100))
    cn(C(100))
    cnn(null)

    return "OK"
}