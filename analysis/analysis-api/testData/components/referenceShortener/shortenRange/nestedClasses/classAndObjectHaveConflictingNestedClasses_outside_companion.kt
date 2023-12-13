package test

class OuterClass {
    class Nested

    companion object {
        class Nested
    }

    <expr>fun usage(
        first: test.OuterClass.Nested,
        second: test.OuterClass.Companion.Nested,
    ) {}</expr>
}