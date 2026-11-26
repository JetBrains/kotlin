// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ResolveEqualsRhsInDependentContextWithCompletion
// ISSUE: KT-92087
// FILE: a.kt
package a

interface Base
interface A<T> : Base
interface Z : Base

fun <T> newA(): A<T> = TODO()
fun takeBoolean(b: Boolean) {}

fun test(z: Z) {
    val b: Boolean <!INITIALIZER_TYPE_MISMATCH!>=<!> z == <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()
    if (<!CONDITION_TYPE_MISMATCH!>z != <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()<!>) {}
    takeBoolean(<!ARGUMENT_TYPE_MISMATCH!>z === <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()<!>)
    <!UNRESOLVED_REFERENCE!>!<!>(z !== <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
}

// FILE: b.kt
package b

interface Base
class DoesNotImplementBase

fun <T, V> gen(func: V): List<T> where T : Base, V : (T) -> Unit = TODO()

fun test(func: (DoesNotImplementBase) -> Unit, list: List<Base>) {
    val b: Boolean <!INITIALIZER_TYPE_MISMATCH!>=<!> list == <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>gen<!>(func)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression,
interfaceDeclaration, intersectionType, localProperty, nullableType, propertyDeclaration, typeConstraint, typeParameter */
