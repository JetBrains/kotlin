// !LANGUAGE: +BareArrayClassLiteral

val a01 = Array::class
val a02 = Array<Array>::class
val a03 = Array<Any?>::class
val a04 = Array<Array<Any?>?>::class
val a05 = Array<IntArray?>::class
val a06 = kotlin.Array::class
val a07 = kotlin.Array<IntArray?>::class
