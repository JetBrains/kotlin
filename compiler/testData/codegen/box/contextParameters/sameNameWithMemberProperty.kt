// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: NATIVE, WASM
// ISSUE: KT-73779
// LANGUAGE: +ContextParameters

class X(val a: String) {
    fun foo(): String { return a }
}

context(a: X)
fun X.function(): String {
    return a.foo()
}

context(a: X)
val X.property: String
    get() = a.foo()

fun box(): String {
    with(X("OK")) {
        if ((X("not OK").function() == "OK") &&
            (X("not OK").property == "OK")
        ) return "OK"
    }
    return "not ok"
}