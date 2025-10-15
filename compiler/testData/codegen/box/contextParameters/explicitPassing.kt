// LANGUAGE: +ContextParameters +ExplicitContextArguments

// MODULE: a
// FILE: a.kt
context(a: String, b: String)
fun fromDifferentModule() = a + b

// MODULE: b(a)
// FILE: b.kt
context(s: String)
fun simple1() = s

context(i: Int, s: String)
fun simple2() = s

context(t: T)
fun <T> generic1() = t

context(t: T, r: R)
fun <T, R> generic2() = r

context(f: () -> String)
fun lambda() = f()

context(f: () -> T)
fun <T> genericLambda() = f()

context(f: (T) -> T)
fun <T> genericLambda(t: T) = f(t)

fun getString() = "OK"

fun stringIdentity(s: String) = s

context(c: String)
fun contextAndValue(s: String) = c + s

fun interface Fun {
    fun invoke(): String
}

context(f: Fun)
fun samConversion() = f.invoke()

fun box(): String {
    if (simple1(s = "OK") != "OK") return "FAIL 1"
    if (simple2(i = 1, s = "OK") != "OK") return "FAIL 2"
    if (generic1(t = "OK") != "OK") return "FAIL 3"
    if (generic1(t = 1) != 1) return "FAIL 4"
    if (generic1<Long>(t = 1) != 1L) return "FAIL 5"
    if (generic2(t = 1, r = "OK") != "OK") return "FAIL 6"
    if (generic2<Long, _>(t = 1, r = "OK") != "OK") return "FAIL 7"
    if (generic2(t = "OK", r = 1) != 1) return "FAIL 8"
    if (generic2<_, Long>(t = "OK", r = 1) != 1L) return "FAIL 9"

    with(1) {
        if (simple2(s = "OK") != "OK") return "FAIL 10"
        if (generic2(r = "OK") != "OK") return "FAIL 11"
        if (generic2(t = "OK") != 1) return "FAIL 12"
    }

    if (lambda(f = { "OK" }) != "OK") return "FAIL 13"

    if (genericLambda<String>(f = { "OK" }) != "OK") return "FAIL 14"
    if (genericLambda<String>(f = ::getString) != "OK") return "FAIL 15"

    if (genericLambda<String>("OK", f = { it }) != "OK") return "FAIL 16"
    if (genericLambda<String>("OK", f = ::stringIdentity) != "OK") return "FAIL 17"
    if (genericLambda("OK", f = { it }) != "OK") return "FAIL 18"
    if (genericLambda("OK", f = ::stringIdentity) != "OK") return "FAIL 19"

    if (contextAndValue(s = "K", c = "O") != "OK") return "FAIL 20"
    if (contextAndValue(c = "O", s = "K") != "OK") return "FAIL 21"

    // test side effects
    var i = 0
    if (contextAndValue(s = run { i = 1; "K" }, c = run { if (i == 1) "O" else "!" }) != "OK") return "FAIL 22"
    if (contextAndValue(c = run { i = 2; "O" }, s = run { if (i == 2) "K" else "!" }) != "OK") return "FAIL 23"

    if (fromDifferentModule(a = "O", b = "K") != "OK") return "FAIL 24"
    if (fromDifferentModule(b = "K", a = "O") != "OK") return "FAIL 25"

    if (samConversion(f = { "OK" }) != "OK") return "FAIL 26"

    return "OK"
}