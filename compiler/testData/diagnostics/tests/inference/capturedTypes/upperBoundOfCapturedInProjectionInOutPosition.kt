// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-64702
class Box<T : CharSequence>(var value: T)

fun test(box: Box<in String>) {
    box.value.length
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, inProjection, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */
