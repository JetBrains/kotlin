class F {
    protected val foo: Int = 0

    protected fun bar() {}

    protected class Nested
}

class G {
    interface H {
        protected val foo: Int = 0

        protected fun bar() {}

        protected class Nested
    }
}

sealed class S {
    protected val foo: Int = 0

    protected fun bar() {}

    protected class Nested : S()

    protected object Obj : S()
}

enum class E {
    SINGLE {
        override val x = foo()
    };

    abstract val x: Int

    protected fun foo() = 42
}
