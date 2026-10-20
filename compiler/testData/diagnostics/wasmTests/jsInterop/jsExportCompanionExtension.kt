// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS

class Carrier {
    companion {
        val prefix = "O"
    }
}

@JsExport
companion fun Carrier.companionExtensionFun(value: String): String = Carrier.prefix + value

<!WRONG_EXPORTED_DECLARATION("extension function")!>@JsExport
fun Carrier.instanceExtensionFun(value: String): String<!> = Carrier.prefix + value
