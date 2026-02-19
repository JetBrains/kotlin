// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// KT-12322 Overload resolution ambiguity with constructor references when class has a companion object

class Foo {
    companion object
}

fun test() {
    val a = ::Foo
    checkSubtype<() -> Foo>(a)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, localProperty, nullableType, objectDeclaration, propertyDeclaration,
typeParameter, typeWithExtension */
