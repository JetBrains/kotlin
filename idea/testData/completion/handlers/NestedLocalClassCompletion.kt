fun foo() {
    class LocalClass {
        inner class Nested {}

        val v: <caret>
    }
}