// !LANGUAGE: -RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file<!SYNTAX!><!> Suppress("")<!> x: Int) {}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file <!SYNTAX!>@<!>Suppress("")<!>
fun test2() {}

class OnType(x: @file<!SYNTAX!><!> Suppress("") Int)

fun @file : Suppress("") Int.test3() {}