// !DIAGNOSTICS: -UNUSED_PARAMETER

class SomeContainer {
    protected class Limit

    protected fun makeLimit(): Limit = TODO()

    public inline fun foo(f: () -> Unit) {
        Limit()
        makeLimit()
    }
}

open class A protected constructor() {
    inline fun foo(f: () -> Unit) {
        A()
    }
}
