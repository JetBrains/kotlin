class Foo private constructor(s: String) {

    private fun foo(s: String) {}

    companion object {
        fun foo() {
            Foo("123").foo("456")
        }
    }
}