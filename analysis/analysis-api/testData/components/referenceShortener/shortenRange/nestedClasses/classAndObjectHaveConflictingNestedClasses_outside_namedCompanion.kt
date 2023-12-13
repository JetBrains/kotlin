package test

class OuterClass {
    class Nested

    companion object NamedCompanion {
        class Nested
    }

    <expr>fun usage(
        first: test.OuterClass.Nested,
        second: test.OuterClass.NamedCompanion.Nested,
    ) {}</expr>
}