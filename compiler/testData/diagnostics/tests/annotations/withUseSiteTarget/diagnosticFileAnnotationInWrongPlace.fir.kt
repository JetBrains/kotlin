package bar

<!MUST_BE_INITIALIZED!><!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:foo<!>
val prop<!>

@file:[<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>bar<!> <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>baz<!>]
fun func() {}

@file:[<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>baz<!>]
class C

<!SYNTAX!>@file:<!>
interface T

@file:[<!SYNTAX!><!>]
interface T

annotation class foo
annotation class bar
annotation class baz