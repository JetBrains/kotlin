// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class X {
    val a = ""
    fun foo(): String { return "OK" }
}

context(a: X)
fun X.test(): String {
    return a.foo()
}

fun box(): String {
    with(X()){
        return X().test()
    }
}