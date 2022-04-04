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

fun ic(): IC = C(100)
fun icn(): IC? = C(100)
fun icnn(): IC? = null
fun any(): Any = C(100)
fun anyN(): Any? = C(100)
fun anyNN(): Any? = null
fun c(): C = C(100)
fun cn(): C? = C(100)
fun cnn(): C? = null
fun i(): I = C(100)
fun iN(): I? = C(100)
fun inn(): I? = null

fun box(): String {
    var res = 1
    res = (ic() as C).ok
    if (res != 100) return "$res"
    res = (ic() as? C)?.ok ?: 11
    if (res != 100) return "$res"
    res = (ic() as? C)?.let { 100 } ?: 21
    if (res != 100) return "$res"
    res = (ic() as? C)?.run { 100 } ?: 31
    if (res != 100) return "$res"
    res = (ic() as? C)?.let { it.ok } ?: 41
    if (res != 100) return "$res"
    res = (ic() as? C)?.run { ok } ?: 51
    if (res != 100) return "$res"

    res = 2
    res = (icn() as C).ok
    if (res != 100) return "$res"
    res = (icn() as? C)?.ok ?: 12
    if (res != 100) return "$res"
    res = (icn() as? C)?.let { 100 } ?: 22
    if (res != 100) return "$res"
    res = (icn() as? C)?.run { 100 } ?: 32
    if (res != 100) return "$res"
    res = (icn() as? C)?.let { it.ok } ?: 42
    if (res != 100) return "$res"
    res = (icn() as? C)?.run { ok } ?: 52
    if (res != 100) return "$res"

    res = 4
    res = (any() as C).ok
    if (res != 100) return "$res"
    res = (any() as? C)?.ok ?: 14
    if (res != 100) return "$res"
    res = (any() as? C)?.let { 100 } ?: 24
    if (res != 100) return "$res"
    res = (any() as? C)?.run { 100 } ?: 34
    if (res != 100) return "$res"
    res = (any() as? C)?.let { it.ok } ?: 44
    if (res != 100) return "$res"
    res = (any() as? C)?.run { ok } ?: 54
    if (res != 100) return "$res"

    res = 5
    res = (anyN() as C).ok
    if (res != 100) return "$res"
    res = (anyN() as? C)?.ok ?: 15
    if (res != 100) return "$res"
    res = (anyN() as? C)?.let { 100 } ?: 25
    if (res != 100) return "$res"
    res = (anyN() as? C)?.run { 100 } ?: 35
    if (res != 100) return "$res"
    res = (anyN() as? C)?.let { it.ok } ?: 45
    if (res != 100) return "$res"
    res = (anyN() as? C)?.run { ok } ?: 55
    if (res != 100) return "$res"

    res = 6
    res = (icnn() as? C)?.let { 16 } ?: 100
    if (res != 100) return "$res"
    res = (icnn() as? C)?.run { 26 } ?: 100
    if (res != 100) return "$res"

    res = 7
    res = (anyNN() as? C)?.let { 17 } ?: 100
    if (res != 100) return "$res"
    res = (anyNN() as? C)?.run { 27 } ?: 100
    if (res != 100) return "$res"

    res = 8
    res = (c() as C).ok
    if (res != 100) return "$res"
    res = (c() as? C)?.ok ?: 18
    if (res != 100) return "$res"
    res = (c() as? C)?.let { 100 } ?: 28
    if (res != 100) return "$res"
    res = (c() as? C)?.run { 100 } ?: 38
    if (res != 100) return "$res"
    res = (c() as? C)?.let { it.ok } ?: 48
    if (res != 100) return "$res"
    res = (c() as? C)?.run { ok } ?: 58
    if (res != 100) return "$res"

    res = 9
    res = (cn() as C).ok
    if (res != 100) return "$res"
    res = (cn() as? C)?.ok ?: 19
    if (res != 100) return "$res"
    res = (cn() as? C)?.let { 100 } ?: 29
    if (res != 100) return "$res"
    res = (cn() as? C)?.run { 100 } ?: 39
    if (res != 100) return "$res"
    res = (cn() as? C)?.let { it.ok } ?: 49
    if (res != 100) return "$res"
    res = (cn() as? C)?.run { ok } ?: 59
    if (res != 100) return "$res"

    res = 0
    res = (cnn() as? C)?.let { 10 } ?: 100
    if (res != 100) return "$res"
    res = (cnn() as? C)?.run { 20 } ?: 100
    if (res != 100) return "$res"

    res = 10
    res = (i() as C).ok
    if (res != 100) return "$res"
    res = (i() as? C)?.ok ?: 110
    if (res != 100) return "$res"
    res = (i() as? C)?.let { 100 } ?: 210
    if (res != 100) return "$res"
    res = (i() as? C)?.run { 100 } ?: 310
    if (res != 100) return "$res"
    res = (i() as? C)?.let { it.ok } ?: 410
    if (res != 100) return "$res"
    res = (i() as? C)?.run { ok } ?: 510
    if (res != 100) return "$res"

    res = 11
    res = (iN() as C).ok
    if (res != 100) return "$res"
    res = (iN() as? C)?.ok ?: 111
    if (res != 100) return "$res"
    res = (iN() as? C)?.let { 100 } ?: 211
    if (res != 100) return "$res"
    res = (iN() as? C)?.run { 100 } ?: 311
    if (res != 100) return "$res"
    res = (iN() as? C)?.let { it.ok } ?: 411
    if (res != 100) return "$res"
    res = (iN() as? C)?.run { ok } ?: 511
    if (res != 100) return "$res"

    res = 12
    res = (inn() as? C)?.let { 112 } ?: 100
    if (res != 100) return "$res"
    res = (inn() as? C)?.run { 212 } ?: 100
    if (res != 100) return "$res"

    return "OK"
}