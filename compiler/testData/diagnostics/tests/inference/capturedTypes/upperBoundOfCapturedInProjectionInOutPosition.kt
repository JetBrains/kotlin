// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-64702
class Box<T : CharSequence>(var value: T)

fun test(box: Box<in String>) {
    box.value.<!UNRESOLVED_REFERENCE!>length<!>
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, inProjection, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */
