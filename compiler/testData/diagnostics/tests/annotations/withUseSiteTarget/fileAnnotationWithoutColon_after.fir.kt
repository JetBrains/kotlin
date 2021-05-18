// !LANGUAGE: +RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(<!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file<!SYNTAX!><!> Suppress("")<!> x: Int) {}

<!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file <!SYNTAX!>@<!>Suppress("")<!>
fun test2() {}

class OnType(x: @file<!SYNTAX!><!> Suppress("") Int)

fun @file : Suppress("") Int.test3() {}