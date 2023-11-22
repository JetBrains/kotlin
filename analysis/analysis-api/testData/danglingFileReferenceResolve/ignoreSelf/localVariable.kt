// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo {
    private fun foo() {
        val name = "Foo"
        nam<caret>e.length
    }
}