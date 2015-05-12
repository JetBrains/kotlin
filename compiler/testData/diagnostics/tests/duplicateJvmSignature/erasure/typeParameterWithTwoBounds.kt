// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
interface Bar

<!CONFLICTING_JVM_DECLARATIONS!>fun <T: Foo> foo(x: T): T<!> where T: Bar {null!!}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Foo): Foo<!> {null!!}
fun foo(x: Bar): Bar {null!!}