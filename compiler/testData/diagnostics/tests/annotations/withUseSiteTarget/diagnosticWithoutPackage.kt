<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:foo<!>
@foo @bar
@<!INAPPLICABLE_FILE_TARGET!>file<!>:[<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>baz<!>]
fun test() {}

annotation class foo
annotation class bar
annotation class baz