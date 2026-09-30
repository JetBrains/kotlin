// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-80721

class Box<X : Number>

val x = object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> {
    fun foo(t: <!UNRESOLVED_REFERENCE!>T<!>) {}
}

fun <R> test() {
    val y = object<!TYPE_PARAMETERS_IN_OBJECT!><T : R, K : Comparable<K>><!> {
        fun foo(t: <!UNRESOLVED_REFERENCE!>T<!>): <!UNRESOLVED_REFERENCE!>K<!> = null!!
    }
}

fun unresolvedBound() {
    val a = object<!TYPE_PARAMETERS_IN_OBJECT!><T : <!UNRESOLVED_REFERENCE!>Unresolved<!>><!> {}
}

fun ownTypeParameterBound() {
    val b = object<!TYPE_PARAMETERS_IN_OBJECT!><T, K : T><!> {}
}

fun cyclicBound() {
    val c = object<!TYPE_PARAMETERS_IN_OBJECT!><<!CYCLIC_GENERIC_UPPER_BOUND!>T : T<!>><!> {}
}

fun finalBound() {
    val d = object<!TYPE_PARAMETERS_IN_OBJECT!><T : <!FINAL_UPPER_BOUND!>String<!>><!> {}
}

fun upperBoundViolation() {
    val e = object<!TYPE_PARAMETERS_IN_OBJECT!><T : <!FINAL_UPPER_BOUND!>Box<<!UPPER_BOUND_VIOLATED!>String<!>><!>><!> {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, checkNotNullCall, classDeclaration, functionDeclaration, localProperty,
nullableType, propertyDeclaration, typeConstraint, typeParameter */
