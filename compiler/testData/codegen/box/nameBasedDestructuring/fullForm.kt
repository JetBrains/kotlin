// LANGUAGE: +NameBasedDestructuring

data class PrimaryCtorProps(
    val pCProp: Int,
    var pCVarProp: String?
)

fun box(): String {
    val source = PrimaryCtorProps(1, "")

    (val pCProp, val pCVarProp) = source
    if (pCProp != 1 || pCVarProp != "") return "FAIL"

    (val number = pCProp, val text = pCVarProp) = source
    if (number != 1 || text != "") return "FAIL"

    (var mutableNumber = pCProp, var mutableText = pCVarProp) = source
    mutableNumber += 1
    if (mutableNumber != 2 || mutableText != "") return "FAIL"

    return "OK"
}
