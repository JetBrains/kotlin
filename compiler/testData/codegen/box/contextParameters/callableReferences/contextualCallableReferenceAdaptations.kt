// LANGUAGE: +ContextParameters +CallableReferencesToContextual

var sink = ""

context(c: String)
fun varargFun(vararg xs: Int): String {
    var sum = 0
    for (x in xs) sum += x
    return c + sum
}

context(c: String)
fun withResult(suffix: String): String {
    sink = c + suffix
    return sink
}

fun box(): String {
    context("ctx") {
        // vararg adapter: (Int, Int) -> String over `vararg xs: Int`
        val two: (Int, Int) -> String = ::varargFun
        if (two(1, 2) != "ctx3") return "FAIL 1: ${two(1, 2)}"

        // vararg adapter with zero unbound arguments: only the bound context argument remains
        val zero: () -> String = ::varargFun
        if (zero() != "ctx0") return "FAIL 2: ${zero()}"

        // Unit-coercion adapter: the return value is dropped, the context argument is still passed
        val u: (String) -> Unit = ::withResult
        sink = ""
        u("!")
        if (sink != "ctx!") return "FAIL 3: $sink"
    }
    return "OK"
}
