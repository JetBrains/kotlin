// LANGUAGE: +ContextParameters +CallableReferencesToContextual

context(c: String)
fun target(suffix: String): String = c + suffix

inline fun callDirect(f: (String) -> String): String = f("K")

inline fun capture(crossinline f: (String) -> String): () -> String = { f("K") }

inline fun callNoinline(noinline f: (String) -> String): String = f("K")

fun box(): String = context("O") {
    if (callDirect(::target) != "OK") return@context "FAIL 1: ${callDirect(::target)}"

    val deferred = capture(::target)
    if (deferred() != "OK") return@context "FAIL 2: ${deferred()}"

    if (callNoinline(::target) != "OK") return@context "FAIL 3: ${callNoinline(::target)}"

    "OK"
}
