/** See [Foo.b<caret>ar] */
class T {

}

class Foo {
    companion object {
        fun bar() {
        }
    }
}

// REF: (in Foo.Companion).bar()
