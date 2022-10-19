// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

interface I

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC: I

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
    res = (a as? C)?.let { it.ok } ?: 41
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 51
    if (res != 100) error(res)
    if (a !is C) error("FAIL 61")
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
    res = (a as? C)?.let { it.ok } ?: 42
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 52
    if (res != 100) error(res)
    if (a !is C) error("FAIL 62")
}

fun icnn(a: IC?) {
    var res = 6
    res = (a as? C)?.let { 16 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 26 } ?: 100
    if (res != 100) error("$res")
    if (a is C) error("FAIL 36")
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
    res = (a as? C)?.let { it.ok } ?: 44
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 54
    if (res != 100) error(res)
    if (a !is C) error("FAIL 64")
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
    res = (a as? C)?.let { it.ok } ?: 45
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 55
    if (res != 100) error(res)
    if (a !is C) error("FAIL 65")
}

fun anyNN(a: Any?) {
    var res = 7
    res = (a as? C)?.let { 17 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 27 } ?: 100
    if (res != 100) error("$res")
    if (a is C) error("FAIL 37")
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
    res = (a as? C)?.let { it.ok } ?: 48
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 58
    if (res != 100) error(res)
    if (a !is C) error("FAIL 68")
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
    res = (a as? C)?.let { it.ok } ?: 49
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 59
    if (res != 100) error(res)
    if (a !is C) error("FAIL 69")
}

fun cnn(a: C?) {
    var res = 0
    res = (a as? C)?.let { 10 } ?: 100
    if (res != 100) error("$res")
    res = (a as? C)?.run { 20 } ?: 100
    if (res != 100) error("$res")
    if (a is C) error("FAIL 30")
}

fun i(a: I) {
    var res = 10
    res = (a as C).ok
    if (res != 100) error(res)
    res = (a as? C)?.ok ?: 110
    if (res != 100) error(res)
    res = (a as? C)?.let { 100 } ?: 210
    if (res != 100) error(res)
    res = (a as? C)?.run { 100 } ?: 310
    if (res != 100) error(res)
    res = (a as? C)?.let { it.ok } ?: 410
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 510
    if (res != 100) error(res)
    if (a !is C) error("FAIL 3A")
}

fun iN(a: I?) {
    var res = 20
    res = (a as C).ok
    if (res != 100) error(res)
    res = (a as? C)?.ok ?: 120
    if (res != 100) error(res)
    res = (a as? C)?.let { 100 } ?: 220
    if (res != 100) error(res)
    res = (a as? C)?.run { 100 } ?: 320
    if (res != 100) error(res)
    res = (a as? C)?.let { it.ok } ?: 420
    if (res != 100) error(res)
    res = (a as? C)?.run { ok } ?: 520
    if (res != 100) error(res)
    if (a !is C) error("FAIL 3B")
}

fun iNN(a: I?) {
    var res = 30
    res = (a as? C)?.let { 130 } ?: 100
    if (res != 100) error(res)
    res = (a as? C)?.run { 230 } ?: 100
    if (res != 100) error(res)
    if (a is C) error("FAIL 3C")
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
    i(C(100))
    iN(C(100))
    iNN(null)

    return "OK"
}