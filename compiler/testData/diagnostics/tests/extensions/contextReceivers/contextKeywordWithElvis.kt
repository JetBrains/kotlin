// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
class Context(val project: Any?)

fun calculateResult(context: Context?) {
    context!!
    val project = <!DEBUG_INFO_SMARTCAST!>context<!>.project!!
}
