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
    (<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().<!FUNCTION_CALL_EXPECTED!>f<!><!><!SYNTAX!><!>
    class C : I<!SYNTAX!><!>

// FILE: c.kt
package c

import kotlin.<!UNRESOLVED_IMPORT!>properties<!>.*
import kotlin.reflect.*
import kotlin.<!UNRESOLVED_IMPORT!>math<!>.*
interface I {
    fun <T : <!FINAL_UPPER_BOUND!>String<!>> f(x: T?) = x ?: "OK".<!UNRESOLVED_REFERENCE!>strip<!>()?.substringBeforeLast('', <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().f<<!UPPER_BOUND_VIOLATED!>Long<!>>(<!ARGUMENT_TYPE_MISMATCH!>-62<!>)<!>)!!
}

class C : I

fun box() = C().f<String>(null)

// FILE: d.kt
package d

interface I {
    fun <T
            > f<!SYNTAX!><!> = <!TOO_MANY_ARGUMENTS!>C<!>(
        <!SYNTAX!><!SYNTAX!><!>.<!><!INFIX_MODIFIER_REQUIRED, TOO_MANY_ARGUMENTS!>f<!><!SYNTAX!><!>
    class C : I<!SYNTAX!><!>

// FILE: e.kt
package e

class A<E<!SYNTAX!><!>
{

    var bar = <!TOO_MANY_ARGUMENTS!>EmptyContinuation<!>(
        <!SYNTAX!><!SYNTAX!><!>.<!><!FUNCTION_EXPECTED!>bar<!><!SYNTAX!><!>

    class EmptyContinuation : A<<!SYNTAX, SYNTAX!><!>
