private class Foo {
    fun method() {}
}

public interface I {
    public fun bar()
}

public fun f() {
    val i = object : I {
        internal var foo = 0
        override fun bar() {}
    }
    i.foo = 1

    class LocalClass {
        internal var foo = 0
    }
    LocalClass().foo = 1
}