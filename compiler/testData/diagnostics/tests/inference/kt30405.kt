// FIR_IDENTICAL
// !LANGUAGE: +ExpectedTypeFromCast
// !CHECK_TYPE
// Issue: KT-30405

inline fun <reified T> foo(): T {
    TODO()
}

fun test() {
    val fooCall = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as String // T in foo should be inferred to String
    fooCall checkType { _<String>() }

    val safeFooCall = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>() as? String
    safeFooCall checkType { _<String?>() }
}
