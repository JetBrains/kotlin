// IS_APPLICABLE: false
class Foo() {
    inner class Inner() {
        fun f(): Any {
            return this@<caret>Foo
        }
    }
}
