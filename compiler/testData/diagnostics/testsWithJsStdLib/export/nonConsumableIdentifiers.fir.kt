// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

@JsExport
fun delete() {}

@JsExport
val instanceof = 4

@JsExport
class eval

@JsExport
@JsName("await")
fun foo() {}

@JsExport
@JsName("this")
val bar = 4

@JsExport
@JsName("super")
class Baz

@JsExport
@JsName("default")
class DefDef

@JsExport
class Test {
    fun instanceof() {}

    @JsName("eval")
    fun test() {}
}

@JsExport
object NaN

@JsExport
enum class Nums {
    Infinity,
    undefined
}
