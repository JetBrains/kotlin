// IS_APPLICABLE: false
// ERROR: Cannot weaken access privilege 'public' for 'foo' in 'T'
trait T {
    public fun foo()
}
abstract class C : T {
    <caret>protected override fun foo() {}
}