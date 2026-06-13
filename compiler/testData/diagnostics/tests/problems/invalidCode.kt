// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47484, KT-47495

// FILE: a.kt
package a

@
<!SYNTAX!>:<!>Suppress(<!NAMED_PARAMETER_NOT_FOUND!>receiver<!> = <!UNRESOLVED_REFERENCE!>iterator<!><!SYNTAX, SYNTAX!><!>

// FILE: b.kt
package b

interface I {
    fun <T
            > f<!SYNTAX!><!> = "".
    (<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().<!CANNOT_INFER_PARAMETER_TYPE, FUNCTION_CALL_EXPECTED!>f<!><!><!SYNTAX!><!>
    class C : I<!SYNTAX!><!>

// FILE: c.kt
package c

import kotlin.<!UNRESOLVED_IMPORT!>properties<!>.*
import kotlin.reflect.*
import kotlin.<!UNRESOLVED_IMPORT!>math<!>.*
interface I {
    fun <T : <!FINAL_UPPER_BOUND!>String<!>> f(x: T?) = x ?: "OK".<!UNRESOLVED_REFERENCE!>strip<!>()?.substringBeforeLast('', <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().<!INAPPLICABLE_CANDIDATE!>f<!><<!UPPER_BOUND_VIOLATED!>Long<!>>(-62)<!>)!!
}

class C : I

fun box() = C().f<String>(null)

// FILE: d.kt
package d

interface I {
    fun <T
            > f<!SYNTAX!><!> = C(
        <!SYNTAX!><!SYNTAX!><!>.<!><!CANNOT_INFER_PARAMETER_TYPE, INFIX_MODIFIER_REQUIRED!>f<!><!SYNTAX!><!>
    class C : I<!SYNTAX!><!>

// FILE: e.kt
package e

class A<E<!SYNTAX!><!>
{

    var bar = EmptyContinuation(
        <!SYNTAX!><!SYNTAX!><!>.<!><!FUNCTION_EXPECTED!>bar<!><!SYNTAX!><!>

    class EmptyContinuation : A<

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, elvisExpression, functionDeclaration, interfaceDeclaration,
nestedClass, nullableType, propertyDeclaration, safeCall, stringLiteral, typeConstraint, typeParameter */<!SYNTAX, SYNTAX!><!>
