// COPY_RESOLUTION_MODE: PREFER_SELF

class Foo {
    private fun foo() {
        val name = "Foo"
        nam<caret>e.length
    }
}