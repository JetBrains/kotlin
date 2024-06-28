// EXCLUDED_CLASSES: one.two/Foo three.four/Bar /Foo /Bar.Nested /Baz.Nested.Nested five.six/Baz
// MODULE: excluded_classes_library

class Foo {
    class Nested {
        class Nested
    }
}
class Bar {
    class Nested {
        class Nested
    }
}
class Baz {
    class Nested {
        class Nested
    }
}