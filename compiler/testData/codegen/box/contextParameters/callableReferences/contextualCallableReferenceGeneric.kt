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
        // generic value parameter, T := Int from the expected type
        val d: (Int) -> String = ::describe
        if (d(1) != "ctx1") return "FAIL 1: ${d(1)}"

        // member of a generic class, bound dispatch receiver
        val bound: () -> String = Box("v")::render
        if (bound() != "ctxv") return "FAIL 2: ${bound()}"

        // member of a generic class, unbound dispatch receiver
        val unbound: (Box<Int>) -> String = Box<Int>::render
        if (unbound(Box(7)) != "ctx7") return "FAIL 3: ${unbound(Box(7))}"

        // generic extension property, bound receiver
        val p: () -> String = 42::tagged
        if (p() != "ctx42") return "FAIL 4: ${p()}"
    }

    context(9) {
        // generic *context* parameter, C := Int inferred from the context argument in scope
        val cs: () -> String = ::contextToString
        if (cs() != "9") return "FAIL 5: ${cs()}"
    }

    return "OK"
}
