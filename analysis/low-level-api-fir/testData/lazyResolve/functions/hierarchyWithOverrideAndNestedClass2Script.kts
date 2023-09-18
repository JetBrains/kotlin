interface Foo1 {
    fun foo()
    fun bar()
    val str: String

    class ClassFromInterface
}

interface Foo2 : Foo1 {
    fun foo(i: Int)
    fun bar(s: String)
    val isBoo: Boolean
}

interface Foo3 : Foo1 {
    fun foo(i: Int)
    fun bar(s: String)
    val isBoo: Boolean
}

abstract class OuterClass : Foo1 {
    class SimpleNestedClass {
        fun foo() {

        }
    }

    abstract class NestedClass : Foo2 {
        override fun foo() {}
    }

    abstract class AnotherNestedClass : NestedClass() {
        override fun b<caret>ar(s: String) {

        }
    }
}
