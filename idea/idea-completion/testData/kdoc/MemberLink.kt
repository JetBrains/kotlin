class Foo {
    /**
     * [<caret>]
     */
    fun xyzzy() {

    }

    fun bar() {

    }
}

fun Foo.quux() {
}

// EXIST: bar
// EXIST: quux
