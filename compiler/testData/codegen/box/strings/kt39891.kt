// KT-39891 - Long in String template causes precision loss (JS)

val l = 4611686018427387904L

fun longFun(): Long = l
fun longNumberFun(): Number = l
fun longAnyFun(): Any = l
fun longComparableFun(): Comparable<Long> = l
fun longOptionalFun(): Long? = l
fun longOptionalFunNull(): Long? = null

fun box(): String {

    if (l.toString() != "4611686018427387904") return "Fail 1"

    if ("" + l != "4611686018427387904") return "Fail 10"
    if ("" + longFun() != "4611686018427387904") return "Fail 11"
    if ("" + longNumberFun() != "4611686018427387904") return "Fail 12"
    if ("" + longAnyFun() != "4611686018427387904") return "Fail 13"
    if ("" + longComparableFun() != "4611686018427387904") return "Fail 14"
    if ("" + longOptionalFun() != "4611686018427387904") return "Fail 15"
    if ("" + longOptionalFunNull() != null.toString()) return "Fail 16"
    if ("" + (longOptionalFunNull() ?: l) != "4611686018427387904") return "Fail 17"

    if ("${l}" != "4611686018427387904") return "Fail 20"
    if ("${longFun()}" != "4611686018427387904") return "Fail 21"
    if ("${longNumberFun()}" != "4611686018427387904") return "Fail 22"
    if ("${longAnyFun()}" != "4611686018427387904") return "Fail 23"
    if ("${longComparableFun()}" != "4611686018427387904") return "Fail 24"
    if ("${longOptionalFun()}" != "4611686018427387904") return "Fail 25"
    if ("${longOptionalFunNull()}" != null.toString()) return "Fail 26"
    if ("${longOptionalFunNull() ?: l}" != "4611686018427387904") return "Fail 27"

    if ("${longFun() + 1}" != "4611686018427387905") return "Fail 30"
    if (longFun() + 1 != 4611686018427387905L) return "Fail 31"

    return "OK"
}