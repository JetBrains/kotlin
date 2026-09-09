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
        val two: (Int, Int) -> String = ::varargFun
        if (two(1, 2) != "ctx3") return "FAIL 1: ${two(1, 2)}"

        val zero: () -> String = ::varargFun
        if (zero() != "ctx0") return "FAIL 2: ${zero()}"

        val u: (String) -> Unit = ::withResult
        sink = ""
        u("!")
        if (sink != "ctx!") return "FAIL 3: $sink"
    }
    return "OK"
}
