// setter: callable: test/FooDelegated.obj
package test

interface Foo<T> {
    var obj: T
}

class FooImpl() : Foo<String> {
    override var obj: String = ""
}

class FooDelegated(delegate: Foo<String>) : Foo<String> by delegate
