package test

fun Any.ext() {}

class Foo

class Outer {
    class Foo

    /**
     * [<caret_1>Foo.ext]
     *
     * [Outer.<caret_2>Foo.ext]
     * [test.Outer.<caret_3>Foo.ext]
     *
     * [test.<caret_4>Foo.ext]
     */
    fun test1() {}

    class Nested {

        class Foo

        /**
         * [<caret_nested_1>Foo.ext]
         *
         * [Nested.<caret_nested_2>Foo.ext]
         * [Outer.Nested.<caret_nested_3>Foo.ext]
         * [test.Outer.Nested.<caret_nested_4>Foo.ext]
         *
         * [test.<caret_nested_5>Foo.ext]
         */
        fun test2() {}
    }
}