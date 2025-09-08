// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A(val x: Int, val y: String)

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration */
