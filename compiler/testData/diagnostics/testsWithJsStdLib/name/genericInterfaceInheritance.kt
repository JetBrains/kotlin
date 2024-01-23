// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport

@JsExport
interface I<T> {
    var x: String
}

@JsExport
interface I2 {
    var x: String
}

@JsExport
abstract class AC : I2 {
    override var x = "AC"
}

@JsExport
open class OC : AC(), I<Int>
