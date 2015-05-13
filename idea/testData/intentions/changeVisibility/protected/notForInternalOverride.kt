// IS_APPLICABLE: false
interface I {
    internal fun foo()
}

abstract class C : I {
    <caret>override fun foo() {}
}