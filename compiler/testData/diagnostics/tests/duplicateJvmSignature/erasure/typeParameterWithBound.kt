// !DIAGNOSTICS: -UNUSED_PARAMETER

trait Foo

<!CONFLICTING_PLATFORM_DECLARATIONS!>fun <T: Foo> foo(x: T): T {null!!}<!>
<!CONFLICTING_PLATFORM_DECLARATIONS!>fun foo(x: Foo): Foo {null!!}<!>