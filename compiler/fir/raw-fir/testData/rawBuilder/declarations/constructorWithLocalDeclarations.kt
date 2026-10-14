package util

class A {
    constructor() {
        class OuterIntoLocal {
            fun doo() = foo()
            fun foo() = bar()
            fun baz() = foo()
        }

        class LocalIntoLocal {
            fun foo() = bar()
            fun bar(): @Anno("bar $prop") List<@Anno("nested bar $prop") Collection<@Anno("nested nested bar $prop") Int>>? = null
            fun foo2() = bar()
        }
    }
}
