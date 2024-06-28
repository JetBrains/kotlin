// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
class Context(val project: Any?)

fun calculateResult(context: Context?) {
    context!!
    val project = context.project!!
}