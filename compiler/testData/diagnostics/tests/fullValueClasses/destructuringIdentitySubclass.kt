// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses

abstract value class Base

data class Identity(val a: Int, val b: Int) : Base()

value class Value(val a: Int, val b: Int)

fun <T : <!FINAL_UPPER_BOUND!>Value<!>> bounded(t: T) {
    val (<!UNRESOLVED_REFERENCE!>first<!>, <!UNRESOLVED_REFERENCE!>second<!>) = t
}

fun test(identity: Identity, value: Value) {
    val (<!UNRESOLVED_REFERENCE!>first<!>, <!UNRESOLVED_REFERENCE!>second<!>) = identity
    val (<!UNRESOLVED_REFERENCE!>third<!>, <!UNRESOLVED_REFERENCE!>fourth<!>) = value
}

/* GENERATED_FIR_TAGS: classDeclaration, data, destructuringDeclaration, functionDeclaration, localProperty,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter, value */
