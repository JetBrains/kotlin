// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo

<!CONFLICTING_OVERLOADS!>fun <T: Foo> foo(x: T): T<!> {null!!}
<!CONFLICTING_OVERLOADS!>fun foo(x: Foo): Foo<!> {null!!}