// FIR_IDENTICAL
// LANGUAGE: -ProhibitNonExhaustiveIfInRhsOfElvis
// ISSUE: KT-51711
// WITH_STDLIB

private fun fake(a: String?) {}

fun test_1(x: String?, y: String?) {
    while (true) {
        x ?: if (y == null) break else fake(y)
    }
}


fun test_2(x: String?, y: String?) {
    while (true) {
        x ?: when {
            true -> break
            else -> y?.filter { it.isLowerCase() }
        }
    }
}
