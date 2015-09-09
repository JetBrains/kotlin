interface A {
    protected fun foo()

    protected fun fooImpl() { }

    protected var bar: Int

    public var baz: String
        public get() = ""
        protected set(value) {}

    fun test(): String {
        foo()
        fooImpl()
        bar = bar + 1
        baz = baz + "1"
        return "OK"
    }
}

class B : A {
    protected override fun foo() {}

    protected override var bar: Int = 42

    override var baz: String = ""
        protected set
}

fun box() = B().test()
