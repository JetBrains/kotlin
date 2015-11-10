open class SuperFoo {
    public fun bar() {
        if (this is Foo) {
            baz()
        }
    }

    public fun baz() {}
}

class Foo : SuperFoo() 

// 0 INVOKEVIRTUAL SuperFoo.baz
// 1 CHECKCAST Foo
// 1 INVOKEVIRTUAL Foo.baz