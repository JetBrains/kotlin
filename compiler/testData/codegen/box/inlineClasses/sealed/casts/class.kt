// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

value class C(val ok: String): IC()

fun box(): String {
    var res = "FAIL 1"
    val ic: IC = C("OK")
    res = (ic as C).ok
    if (res != "OK") return res

    res = "FAIL 2"
    val icn: IC? = ic
    res = (icn as C).ok
    if (res != "OK") return res

    res = "FAIL 3"
    val ic2 = (icn as IC)
    res = (ic2 as C).ok
    if (res != "OK") return res

    return "OK"
}