// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS
import kotlin.js.JsExport

@JsExport
class ClassA<T : UpperBoundInterface>  {
    inner class InnerA {

    }
}

@JsExport
interface UpperBoundInterface {}
