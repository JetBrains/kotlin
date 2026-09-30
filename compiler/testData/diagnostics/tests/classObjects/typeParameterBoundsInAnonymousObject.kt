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
    val a = object<!TYPE_PARAMETERS_IN_OBJECT!><T : Unresolved><!> {}
}

fun ownTypeParameterBound() {
    val b = object<!TYPE_PARAMETERS_IN_OBJECT!><T, K : T><!> {}
}

fun cyclicBound() {
    val c = object<!TYPE_PARAMETERS_IN_OBJECT!><T : T><!> {}
}

fun finalBound() {
    val d = object<!TYPE_PARAMETERS_IN_OBJECT!><T : String><!> {}
}

fun upperBoundViolation() {
    val e = object<!TYPE_PARAMETERS_IN_OBJECT!><T : Box<String>><!> {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, checkNotNullCall, classDeclaration, functionDeclaration, localProperty,
nullableType, propertyDeclaration, typeConstraint, typeParameter */
