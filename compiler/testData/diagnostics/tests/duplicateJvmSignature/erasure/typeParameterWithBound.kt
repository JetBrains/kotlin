// !DIAGNOSTICS: -UNUSED_PARAMETER

trait Foo

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Foo> foo(x: T): T<!> {null!!}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Foo): Foo<!> {null!!}