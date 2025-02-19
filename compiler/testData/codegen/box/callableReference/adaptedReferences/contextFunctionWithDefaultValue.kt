// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
class C(var a: String) {
    fun foo(): String { return a }
}

fun funWithContextAndValueType(x: context(C) () -> String): String {
    with(C("OK")) {
        return x()
    }
}

fun valueParamFunWithDefault(c: C = C("NOT OK")): String { return c.foo() }

fun box(): String {
    return funWithContextAndValueType(::valueParamFunWithDefault)
}