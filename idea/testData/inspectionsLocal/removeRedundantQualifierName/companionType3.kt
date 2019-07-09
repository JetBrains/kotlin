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
    class CheckClass

    fun foo(a: my.simple.name<caret>.Bar.Foo.Companion.CheckClass) {

    }
}