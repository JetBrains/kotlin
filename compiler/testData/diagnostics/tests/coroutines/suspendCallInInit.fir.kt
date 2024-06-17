suspend fun foo() {}

suspend fun test() {
    class Foo {
        init {
            foo()
        }

        val prop = foo()
    }
}