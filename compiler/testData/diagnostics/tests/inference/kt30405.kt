// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast
// !CHECK_TYPE
// Issue: KT-30405

inline fun <reified T> foo(): T {
    TODO()
}

fun test() {
    val fooCall = foo() as String // T in foo should be inferred to String
    fooCall checkType { _<String>() }

    val safeFooCall = foo() as? String
    safeFooCall checkType { _<String?>() }
}