// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

data class A(val x: Int)

fun foo(a: A) {
    checkSubtype<Int>(a.component1())
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
nullableType, primaryConstructor, propertyDeclaration, typeParameter, typeWithExtension */
