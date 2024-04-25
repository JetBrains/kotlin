// WITH_STDLIB
// LANGUAGE: -ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-50520

fun box(): String {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        val foo = { <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>first<!>() }
        add(0, foo)
    }
    return "OK"
}
