// JVM_DEFAULT_MODE: disable

interface Foo {
    private fun foo() {}

    fun bar() {
        foo()
    }

    private var privateProperty: Int
        get() = 0
        set(value) {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Foo.class[foo;privateProperty]
