// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo {
    private class Bar {
        fun call() {}
    }

    private fun foo() {
        B<caret>ar().call()
    }
}