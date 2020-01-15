// !LANGUAGE: -BareArrayClassLiteral -ProhibitGenericArrayClassLiteral

val a01 = <!ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT!>Array::class<!>
val a02 = Array<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Array<!>>::class
val a03 = Array<Any?>::class
val a04 = Array<Array<Any?>?>::class
val a05 = Array<IntArray?>::class
val a06 = <!ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT!>kotlin.Array::class<!>
val a07 = kotlin.Array<IntArray?>::class
