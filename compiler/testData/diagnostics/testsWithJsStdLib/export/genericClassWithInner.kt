// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
import kotlin.js.JsExport

@JsExport
class ClassA<T : UpperBoundInterface>  {
    inner class InnerA {

    }
}

@JsExport
interface UpperBoundInterface {}
