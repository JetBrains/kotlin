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

class Check(ic: IC) {
    val isICAny = ic is ICAny

    val isICC = ic is ICC

    val isICO = ic is ICO
}

fun box(): String {
    var res = check(ICAny("OK"))
    if (res != "OK") return "FAIL 1 $res"

    res = check(ICC("OK"))
    if (res != "OK") return "FAIL 2 $res"

    res = check(ICO)
    if (res != "OK") return "FAIL 3 $res"

    if (!Check(ICAny("OK")).isICAny) return "FAIL 4"
    if (Check(ICAny("OK")).isICC) return "FAIL 41"
    if (Check(ICAny("OK")).isICO) return "FAIL 42"

    if (!Check(ICC("OK")).isICC) return "FAIL 5"
    if (Check(ICC("OK")).isICAny) return "FAIL 51"
    if (Check(ICC("OK")).isICO) return "FAIL 52"

    if (!Check(ICO).isICO) return "FAIL 6"
    if (Check(ICO).isICAny) return "FAIL 61"
    if (Check(ICO).isICC) return "FAIL 62"

    return "OK"
}