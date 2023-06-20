// ORIGINAL: /compiler/testData/diagnostics/tests/redeclarations/DuplicateParameterNamesInFunctionType.fir.kt
// WITH_STDLIB
fun test0(f: (String, String) -> Unit) {
    f("", "")
}

fun test1(f: (a: Int, a: Int) -> Unit) {
    f(1, 1)
}

fun box() = "OK"
