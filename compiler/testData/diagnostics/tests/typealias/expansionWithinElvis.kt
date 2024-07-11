// FIR_IDENTICAL
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
// ISSUE: KT-69298

typealias UntypedValue = Any?
fun randomValue(): UntypedValue = ""

fun main() {
    val value: Any = randomValue() ?: "<<null>>"
    value.toString()
}

fun test(): String {
    val value: Any = randomValue() ?: return "<<null>>"
    return value.toString()
}
