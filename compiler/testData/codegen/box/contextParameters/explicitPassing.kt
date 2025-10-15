// LANGUAGE: +ContextParameters +ExplicitPassingOfContextParameters
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

fun box(): String {
    if (simple1(s = "OK") != "OK") return "FAIL 1"
    if (simple2(i = 1, s = "OK") != "OK") return "FAIL 2"
    if (generic1(t = "OK") != "OK") return "FAIL 3"
    if (generic1(t = 1) != 1) return "FAIL 4"
    if (generic1<Long>(t = 1) != 1L) return "FAIL 4"
    if (generic2(t = 1, r = "OK") != "OK") return "FAIL 5"
    if (generic2<Long, _>(t = 1, r = "OK") != "OK") return "FAIL 5"
    if (generic2(t = "OK", r = 1) != 1) return "FAIL 6"
    if (generic2<_, Long>(t = "OK", r = 1) != 1L) return "FAIL 6"

    with(1) {
        if (simple2(s = "OK") != "OK") return "FAIL 7"
        if (generic2(r = "OK") != "OK") return "FAIL 8"
        if (generic2(t = "OK") != 1) return "FAIL 9"
    }

    if (lambda(f = { "OK" }) != "OK") return "FAIL 10"

    if (genericLambda<String>(f = { "OK" }) != "OK") return "FAIL 10"
    if (genericLambda<String>(f = ::getString) != "OK") return "FAIL 10"

    if (genericLambda<String>("OK", f = { it }) != "OK") return "FAIL 10"
    if (genericLambda<String>("OK", f = ::stringIdentity) != "OK") return "FAIL 10"
    if (genericLambda("OK", f = { it }) != "OK") return "FAIL 10"
    if (genericLambda("OK", f = ::stringIdentity) != "OK") return "FAIL 10"

    return "OK"
}