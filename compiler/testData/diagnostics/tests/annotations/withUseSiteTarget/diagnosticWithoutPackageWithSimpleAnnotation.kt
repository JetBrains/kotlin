<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:foo<!>
@foo @bar
<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@<!INAPPLICABLE_FILE_TARGET!>file<!>: baz<!>
fun test() {}

annotation class foo
annotation class bar
annotation class baz