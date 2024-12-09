// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// LANGUAGE: +ContextParameters

class X(val a: String) {
    fun foo(): String { return a }
}

context(a: X)
fun X.test(): String {
    return a.foo()
}

fun box(): String {
    with(X("OK")){
        return X("not OK").test()
    }
}