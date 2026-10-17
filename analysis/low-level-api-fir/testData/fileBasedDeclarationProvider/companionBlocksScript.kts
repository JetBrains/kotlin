// CLASS: test/Foo.Bar
// CLASS: test/Foo.Bar.Baz
// CLASS: test/Foo.Qux
// TYPE_ALIAS: test/Foo.MyString

package test

class Foo {
    companion {
        class Bar {
            companion {
                object Baz
            }
        }

        typealias MyString = String

        companion {
            class Qux
        }
    }
}
