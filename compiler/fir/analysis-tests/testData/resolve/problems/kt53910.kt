// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-53910
// WITH_STDLIB

// KT-53910: False negative "Unresolved reference" for callable reference argument to overloaded function with NONE_APPLICABLE error

fun overloadedFunction(vararg args: Int, func: () -> Unit) {}
fun overloadedFunction(args: Iterable<Int>, func: () -> Unit) {}

// referencedFunction is intentionally declared in a comment to trigger the bug in K1
//fun referencedFunction(): Unit

fun foo() {
    // Bug: ::referencedFunction should produce UNRESOLVED_REFERENCE but doesn't
    overloadedFunction(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_PASSED_TWICE!>func<!> = ::<!UNRESOLVED_REFERENCE!>referencedFunction<!>)
    overloadedFunction(listOf(1, 2), func = ::<!UNRESOLVED_REFERENCE!>referencedFunction<!>)
    overloadedFunction(<!ARGUMENT_TYPE_MISMATCH!>"a"<!>, <!ARGUMENT_TYPE_MISMATCH!>"b"<!>, <!ARGUMENT_PASSED_TWICE!>func<!> = ::<!UNRESOLVED_REFERENCE!>referencedFunction<!>)
    overloadedFunction(listOf("a", "b"), func = ::<!UNRESOLVED_REFERENCE!>referencedFunction<!>)

    // These correctly produce UNRESOLVED_REFERENCE
    overloadedFunction(1, 2, func = <!UNRESOLVED_REFERENCE!>itIsNotEvenDecalredButAlsoIsNotReferenced<!>)
    overloadedFunction(listOf(1, 2), func = <!UNRESOLVED_REFERENCE!>itIsNotEvenDecalredButAlsoIsNotReferenced<!>)
    <!NONE_APPLICABLE!>overloadedFunction<!>("a", "b", func = <!UNRESOLVED_REFERENCE!>itIsNotEvenDecalredButAlsoIsNotReferenced<!>)
    overloadedFunction(listOf("a", "b"), func = <!UNRESOLVED_REFERENCE!>itIsNotEvenDecalredButAlsoIsNotReferenced<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, stringLiteral, vararg */
