open class SuperFoo {
    public fun bar() {
        if (this is Foo) {
            baz()
        }
    }

    public fun baz() {}
}

class Foo : SuperFoo() 

// 1 INVOKEVIRTUAL SuperFoo.baz
// 0 CHECKCAST Foo
// 0 INVOKEVIRTUAL Foo.baz
