// LANGUAGE: +ContextParameters +CallableReferencesToContextual

context(c: String)
fun <T> describe(x: T): String = c + x

context(c: C)
fun <C> contextToString(): String = c.toString()

class Box<T>(val v: T) {
    context(c: String)
    fun render(): String = c + v
}

context(c: String)
val <T> T.tagged: String
    get() = c + this

fun box(): String {
    context("ctx") {
        val d: (Int) -> String = ::describe
        if (d(1) != "ctx1") return "FAIL 1: ${d(1)}"

        val bound: () -> String = Box("v")::render
        if (bound() != "ctxv") return "FAIL 2: ${bound()}"

        val unbound: (Box<Int>) -> String = Box<Int>::render
        if (unbound(Box(7)) != "ctx7") return "FAIL 3: ${unbound(Box(7))}"

        val p: () -> String = 42::tagged
        if (p() != "ctx42") return "FAIL 4: ${p()}"
    }

    context(9) {
        val cs: () -> String = ::contextToString
        if (cs() != "9") return "FAIL 5: ${cs()}"
    }

    return "OK"
}
