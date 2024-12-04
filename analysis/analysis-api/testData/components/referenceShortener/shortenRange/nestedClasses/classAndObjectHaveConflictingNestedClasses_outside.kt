package test

class OuterClass {
    class Nested

    object NestedObject {
        class Nested
    }

    <expr>fun usage(
        first: test.OuterClass.Nested,
        second: test.OuterClass.NestedObject.Nested,
    ) {}</expr>
}