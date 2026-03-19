// getter: callable: test/FooDelegated.obj
package test

interface Foo<T> {
    val obj: T
}

class FooImpl() : Foo<String> {
    override val obj: String
        get() = ""
}

class FooDelegated(delegate: Foo<String>) : Foo<String> by delegate
