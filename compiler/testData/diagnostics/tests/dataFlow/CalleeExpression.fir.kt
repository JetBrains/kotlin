// RUN_PIPELINE_TILL: BACKEND
class C(val f : () -> Unit)

fun test(e : Any) {
    if (e is C) {
        (e.f)()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, ifExpression, isExpression,
primaryConstructor, propertyDeclaration, smartcast */
