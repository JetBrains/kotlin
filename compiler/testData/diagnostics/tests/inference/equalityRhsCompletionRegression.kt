// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ResolveEqualsRhsInDependentContextWithCompletion
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// ISSUE: KT-92087
// FILE: a.kt
package a

interface Base
interface A<T> : Base
interface Z : Base

fun <T> newA(): A<T> = TODO()
fun takeBoolean(b: Boolean) {}

fun test(z: Z) {
    val b: Boolean = z == <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()
    if (z != <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>()) {}
    takeBoolean(z === <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
    !(z !== <!CANNOT_INFER_PARAMETER_TYPE!>newA<!>())
}

// FILE: b.kt
package b

interface Base
class DoesNotImplementBase

fun <T, V> gen(func: V): List<T> where T : Base, V : (T) -> Unit = TODO()

fun test(func: (DoesNotImplementBase) -> Unit, list: List<Base>) {
    val b: Boolean = list == <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>gen<!>(func)
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression,
interfaceDeclaration, intersectionType, localProperty, nullableType, propertyDeclaration, typeConstraint, typeParameter */
