/**
 * @see Foo.ba<caret>r
 */
fun xyzzy() {
}

class Foo {
    fun bar() {
    }
}

// REF: (in Foo).bar()
