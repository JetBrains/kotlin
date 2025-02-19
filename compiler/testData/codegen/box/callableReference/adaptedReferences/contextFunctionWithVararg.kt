// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
class C(var a: String) {
    fun foo(): String { return a }
}

fun funWithContextAndValueType(x: context(C) (C) -> String): String {
    with(C("OK")) {
        return x(C("not OK"))
    }
}

fun funWithVararg(vararg c: C): String { return c[0].foo() }

fun box(): String {
    return funWithContextAndValueType(::funWithVararg)
}