class Context(val project: Any?)

fun calculateResult(context: Context?) {
    context!!
    val project = context.project!!
}