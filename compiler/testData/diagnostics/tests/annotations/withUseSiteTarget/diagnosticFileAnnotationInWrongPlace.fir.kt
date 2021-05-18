package bar

<!MUST_BE_INITIALIZED!><!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:foo<!>
val prop<!>

@file:[<!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>bar<!> <!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>baz<!>]
fun func() {}

@file:[<!INAPPLICABLE_FILE_TARGET, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>baz<!>]
class C

<!SYNTAX!>@file:<!>
interface T

@file:[<!SYNTAX!><!>]
interface T

annotation class foo
annotation class bar
annotation class baz