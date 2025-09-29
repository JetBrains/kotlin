// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74999
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION

interface Marker<M : Marker<M>>
class MyClass<C : Marker<C>>

private fun <F : Marker<F>> myFunction(arg: MyClass<F>?): MyClass<F> {
    val result = arg ?: MyClass()
    return result
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, elvisExpression, functionDeclaration, interfaceDeclaration,
localProperty, nullableType, outProjection, propertyDeclaration, starProjection, typeConstraint, typeParameter */
