// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS

@JsExport
class PublicClass {
    class NestedPublicClass {}
    internal class NestedInternalClass {}
    private class NestedPrivateClass {}

    inner class InnerPublicClass {}
    internal inner class InnerInternalClass {}
    private inner class InnerPrivateClass {}

    fun publicFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}

    var publicVar = 42
    internal var internalVar = 42
    private var privateVar = 42

    object NestedPublicObject {}
    internal object NestedInternalObject {}
    private object NestedPrivateObject {}
}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal class InternalClass {}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private class PrivateClass {}

@JsExport
data class PublicDataClass(val data: Int)

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal data class InternalDataClass<!NON_EXPORTABLE_TYPE("return; InternalDataClass")!>(val data: Boolean)<!>

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private data class PrivateDataClass<!NON_EXPORTABLE_TYPE("return; PrivateDataClass")!>(val data: Boolean)<!>

@JsExport
fun publicFun() {}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal fun internalFun() {}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private fun privateFun() {}

@JsExport
val publicVal = 42

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal val internalVal = 42

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private val privateVal = 42

@JsExport
var publicVar = 42

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal var internalVar = 42

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private var privateVar = 42

@JsExport
object PublicObject {
    class NestedPublicClass {}
    internal class NestedInternalClass {}
    private class NestedPrivateClass {}

    fun publicFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}

    var publicVar = 42
    internal var internalVar = 42
    private var privateVar = 42

    object NestedPublicObject {}
    internal object NestedInternalObject {}
    private object NestedPrivateObject {}
}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal object InternalObject {}

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private object PrivateObject {}
