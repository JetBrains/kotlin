// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-25432

class Data<T>(val s: T)

fun test(d: Data<out Any>) {
    if (d.s is String) {
        <!DEBUG_INFO_SMARTCAST!>d.s<!>.length
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, ifExpression, intersectionType, isExpression,
nullableType, outProjection, primaryConstructor, propertyDeclaration, smartcast, typeParameter */
