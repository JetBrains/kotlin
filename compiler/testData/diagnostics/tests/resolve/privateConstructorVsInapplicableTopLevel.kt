// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-72706
class A private constructor(x: Boolean, y: Int)

fun A(y: Int) {}

fun test() {
    A(<!NO_VALUE_FOR_PARAMETER("y")!><!ARGUMENT_TYPE_MISMATCH("String; Boolean")!>"1"<!>)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nullableType,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
