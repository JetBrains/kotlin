package test

class Foo

fun Foo.ext() {}

class WithMember {
    fun Foo() {}

    /**
     * [Foo.<caret_1>ext]
     * [<caret_2>Foo.ext]
     *
     * [<caret_3>Foo]
     */
    fun usage() {}
}