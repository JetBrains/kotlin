// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69773
// WITH_STDLIB

interface MyList<out E> : List<E>

fun test(a: List<Any?>) = a is <!NO_TYPE_ARGUMENTS_ON_RHS!>MyList<!>
