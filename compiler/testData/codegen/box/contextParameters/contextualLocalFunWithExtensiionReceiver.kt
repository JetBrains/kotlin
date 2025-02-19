// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature
class A(val a: String)
class B(val b: String)

fun box(): String {
    context(a: A)
    fun B.localFun(): String {
        return this.b + a.a
    }

    with(A("K")) {
        with(B("O")) {
            return localFun()
        }
    }
}