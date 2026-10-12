// JVM_DEFAULT_MODE: no-compatibility

interface Foo {
    private fun foo() {}

    fun bar() {
        foo()
    }

    private var privateProperty: Int
        get() = 0
        set(value) {}
}
