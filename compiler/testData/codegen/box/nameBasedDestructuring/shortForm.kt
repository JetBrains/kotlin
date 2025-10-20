// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

data class PrimaryCtorProps(
    val pCProp: Int,
    var pCVarProp: String?
)

fun box(): String {
    val source = PrimaryCtorProps(1, "")

    val (pCProp, pCVarProp) = source
    if (pCProp != 1 || pCVarProp != "") return "FAIL"

    val (number = pCProp, text = pCVarProp) = source
    if (number != 1 || text != "") return "FAIL"

    var (mutableNumber = pCProp, mutableText = pCVarProp) = source
    mutableNumber += 1
    if (mutableNumber != 2 || mutableText != "") return "FAIL"

    return "OK"
}
