// callable: test/FooDelegated.obj
package test

interface Foo {
    val obj: String
}

class FooImpl() : Foo {
    override val obj: String
        get() = ""
}

class FooDelegated(delegate: Foo) : Foo by delegate