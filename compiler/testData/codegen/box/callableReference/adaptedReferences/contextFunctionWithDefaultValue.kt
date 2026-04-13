// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters
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
