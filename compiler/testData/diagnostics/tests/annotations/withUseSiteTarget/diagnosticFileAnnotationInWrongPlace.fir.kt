package bar

<!MUST_BE_INITIALIZED!>@file:foo
val prop<!>

@file:[bar baz]
fun func() {}

@file:[baz]
class C

<!SYNTAX!>@file:<!>
interface T

@file:[<!SYNTAX!><!>]
interface T

annotation class foo
annotation class bar
annotation class baz