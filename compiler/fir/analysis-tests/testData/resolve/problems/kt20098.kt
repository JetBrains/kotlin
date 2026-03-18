// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-20098

// KT-20098: AE "Recursion detected on input" after referring to itself when declaring Pair
val <!SYNTAX!>(a, b)<!> = <!UNRESOLVED_REFERENCE!>a<!>
