// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
class Context(val project: Any?)

fun calculateResult(context: Context?) {
    context!!
    val project = context.project!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, localProperty, nullableType,
primaryConstructor, propertyDeclaration, smartcast */
