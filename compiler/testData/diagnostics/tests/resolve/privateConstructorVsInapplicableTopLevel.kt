// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-72706
class A private constructor(x: Boolean, y: Int)

fun A(y: Int) {}

fun test() {
    A(<!ARGUMENT_TYPE_MISMATCH("String; Int")!>"1"<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nullableType,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
