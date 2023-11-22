// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo(val name: String) {
    private fun foo() {
        nam<caret>e.length
    }
}