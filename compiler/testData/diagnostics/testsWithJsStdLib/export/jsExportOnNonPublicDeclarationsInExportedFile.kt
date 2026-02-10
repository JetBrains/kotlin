// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS
@file:JsExport

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

internal class InternalClass {}

private class PrivateClass {}

data class PublicDataClass(val data: Int)

internal data class InternalDataClass<!NON_EXPORTABLE_TYPE("return; InternalDataClass")!>(val data: Boolean)<!>

private data class PrivateDataClass<!NON_EXPORTABLE_TYPE("return; PrivateDataClass")!>(val data: Boolean)<!>

fun publicFun() {}

internal fun internalFun() {}

private fun privateFun() {}

val publicVal = 42

internal val internalVal = 42

private val privateVal = 42

var publicVar = 42

internal var internalVar = 42

private var privateVar = 42

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

internal object InternalObject {}

private object PrivateObject {}
