// WITH_STDLIB

fun getValue() : String? = null
suspend fun computeValue() = "O"

suspend fun repro() : String {
    val value = getValue()
    return if (value == null) {
        computeValue()
    } else {
        value
    } + "K"
}

// This test is checking that the local variable table for `repro` is valid.
// This is checked because the D8 dexer is run on the produced code and
// we fail the tests on warnings because of invalid locals.
fun box() = "OK"
