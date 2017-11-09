// !LANGUAGE: -SingleUnderscoreForParameterName
// See KT-8813, KT-9631

fun someApi(f: (Int) -> Unit) = f(42)

fun test() {
    someApi(fun(p: Int) {})
    // Apparently "p" cannot be removed because the signature is fixed by "someApi
}