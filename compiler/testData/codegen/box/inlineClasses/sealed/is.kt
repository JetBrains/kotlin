// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICAny(val s: Any): IC()

value class ICC(val s: String): IC()

value object ICO: IC()

fun check(ic: IC): String = when(ic) {
    is ICAny -> ic.s as String
    is ICC -> ic.s
    is ICO -> "OK"
}

fun box(): String {
    var res = check(ICAny("OK"))
    if (res != "OK") return "FAIL 1 $res"

    res = check(ICC("OK"))
    if (res != "OK") return "FAIL 2 $res"

    res = check(ICO)
    if (res != "OK") return "FAIL 3 $res"

    return "OK"
}