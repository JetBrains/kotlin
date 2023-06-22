// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
interface Bar

<!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>fun <T> foo(x: T): T<!> where T: Foo, T: Bar {null!!}<!>
<!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>fun foo(x: Foo): Foo<!> {null!!}<!>
fun foo(x: Bar): Bar {null!!}
