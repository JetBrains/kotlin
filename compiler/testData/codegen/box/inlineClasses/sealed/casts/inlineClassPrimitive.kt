// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val ok: Int): IC()

fun box(): String {
    var res = 1
    val ic: IC = C(100)
    res = (ic as C).ok
    if (res != 100) return "FAIL $res"
    res = (ic as? C)?.ok ?: 11
    if (res != 100) return "FAIL $res"
    res = (ic as? C)?.let { 100 } ?: 21
    if (res != 100) return "FAIL $res"
    res = (ic as? C)?.run { 100 } ?: 31
    if (res != 100) return "FAIL $res"
    res = (ic as? C)?.let { it.ok } ?: 41
    if (res != 100) return "FAIL $res"
    res = (ic as? C)?.run { ok } ?: 51
    if (res != 100) return "FAIL $res"

    res = 2
    val icn: IC? = ic
    res = (icn as C).ok
    if (res != 100) return "FAIL $res"
    res = (icn as? C)?.ok ?: 12
    if (res != 100) return "FAIL $res"
    res = (icn as? C)?.let { 100 } ?: 22
    if (res != 100) return "FAIL $res"
    res = (icn as? C)?.run { 100 } ?: 32
    if (res != 100) return "FAIL $res"
    res = (icn as? C)?.let { it.ok } ?: 42
    if (res != 100) return "FAIL $res"
    res = (icn as? C)?.run { ok } ?: 52
    if (res != 100) return "FAIL $res"

    res = 3
    val ic2 = (icn as IC)
    res = (ic2 as C).ok
    if (res != 100) return "FAIL $res"
    res = (ic2 as? C)?.ok ?: 13
    if (res != 100) return "FAIL $res"
    res = (ic2 as? C)?.let { 100 } ?: 23
    if (res != 100) return "FAIL $res"
    res = (ic2 as? C)?.run { 100 } ?: 33
    if (res != 100) return "FAIL $res"
    res = (ic2 as? C)?.let { it.ok } ?: 43
    if (res != 100) return "FAIL $res"
    res = (ic2 as? C)?.run { ok } ?: 53
    if (res != 100) return "FAIL $res"

    res = 4
    val any = (icn as Any)
    res = (any as C).ok
    if (res != 100) return "FAIL $res"
    res = (any as? C)?.ok ?: 14
    if (res != 100) return "FAIL $res"
    res = (any as? C)?.let { 100 } ?: 24
    if (res != 100) return "FAIL $res"
    res = (any as? C)?.run { 100 } ?: 34
    if (res != 100) return "FAIL $res"
    res = (any as? C)?.let { it.ok } ?: 44
    if (res != 100) return "FAIL $res"
    res = (any as? C)?.run { ok } ?: 54
    if (res != 100) return "FAIL $res"

    res = 5
    val anyN = (icn as Any?)
    res = (anyN as C).ok
    if (res != 100) return "FAIL $res"
    res = (anyN as? C)?.ok ?: 15
    if (res != 100) return "FAIL $res"
    res = (anyN as? C)?.let { 100 } ?: 25
    if (res != 100) return "FAIL $res"
    res = (anyN as? C)?.run { 100 } ?: 35
    if (res != 100) return "FAIL $res"
    res = (anyN as? C)?.let { it.ok } ?: 45
    if (res != 100) return "FAIL $res"
    res = (anyN as? C)?.run { ok } ?: 55
    if (res != 100) return "FAIL $res"

    res = 6
    val icnn: IC? = null
    res = (icnn as? C)?.let { 16 } ?: 100
    if (res != 100) return "FAIL $res"
    res = (icnn as? C)?.run { 26 } ?: 100
    if (res != 100) return "FAIL $res"

    res = 7
    val anyNN: Any? = null
    res = (anyNN as? C)?.let { 17 } ?: 100
    if (res != 100) return "FAIL $res"
    res = (anyNN as? C)?.run { 27 } ?: 100
    if (res != 100) return "FAIL $res"

    return "OK"
}