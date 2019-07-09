// PROBLEM: none
package my.simple.name

class Foo {
    companion object {
        fun say(){}
    }
}

class Bar {
    class Inner {
        fun a() {
            my.simple<caret>.name.Foo.say()
        }

        class Foo
    }
}
