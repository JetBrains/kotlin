// IS_APPLICABLE: false
trait I {
    fun foo()
}

class C : I {
    override fun <caret>foo() {
    }
}