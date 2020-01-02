// !LANGUAGE: -RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(@file<!SYNTAX!><!> Suppress("") x: Int) {}

@file <!SYNTAX!>@<!>Suppress("")
fun test2() {}

class OnType(x: @file<!SYNTAX!><!> Suppress("") Int)

fun @file : Suppress("") Int.test3() {}