// callable: test/FooDelegated.call
package test

interface Foo {
    fun call(text: String)
}

class FooImpl() : Foo {
    override fun call(text: String) {}
}

class FooDelegated(delegate: Foo) : Foo by delegate