// EXCLUDED_CLASSES: one.two/Foo three.four/Bar excluded_classes.test/Foo excluded_classes.test/Bar.Nested excluded_classes.test/Baz.Nested.Nested five.six/Baz
// MODULE: excluded_classes_library

package excluded_classes.test

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