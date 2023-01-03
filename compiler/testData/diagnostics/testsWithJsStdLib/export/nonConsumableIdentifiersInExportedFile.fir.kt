// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
@file:JsExport

package foo

fun delete() {}

val instanceof = 4

class eval

@JsName("await")
fun foo() {}

@JsName("this")
val bar = 4

@JsName("super")
class Baz

class Test {
    fun instanceof() {}

    @JsName("eval")
    fun test() {}
}

object NaN

enum class Nums {
    Infinity,
    undefined
}
