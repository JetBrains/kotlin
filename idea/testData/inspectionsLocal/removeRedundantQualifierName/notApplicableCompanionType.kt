// PROBLEM: none
package my.simple.name

import my.simple.name.Bar.Foo.Companion.CheckClass

class Bar {
    class Foo {
        companion object {
            class CheckClass
        }
    }
}

class F {
    fun foo(a: Bar<caret>.Foo.Companion.CheckClass) {}

    companion object {
        class CheckClass
    }
}
