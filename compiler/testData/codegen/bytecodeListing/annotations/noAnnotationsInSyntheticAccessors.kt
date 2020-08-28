annotation class Ann

class Foo private @Ann constructor(@Ann s: String) {

    @Ann
    private fun foo(@Ann s: String) {}

    companion object {
        fun foo() {
            Foo("123").foo("456")
        }
    }
}