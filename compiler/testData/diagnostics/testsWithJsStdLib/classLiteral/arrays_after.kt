// FIR_IDENTICAL
// !LANGUAGE: +BareArrayClassLiteral +ProhibitGenericArrayClassLiteral

val a01 = Array::class
val a02 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Array<!>>::class<!>
val a03 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Any?>::class<!>
val a04 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Array<Any?>?>::class<!>
val a05 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<IntArray?>::class<!>
val a06 = kotlin.Array::class
val a07 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>kotlin.Array<IntArray?>::class<!>
