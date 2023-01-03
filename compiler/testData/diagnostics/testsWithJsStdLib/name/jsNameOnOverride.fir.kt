package foo

open class A {
    @JsName("foo_") open fun foo() = 23

    @JsName("bar_") open val bar = 123

    open val baz: Int
        @JsName("getBaz_") get() = 55
}

class B : A() {
    @JsName("foo__") override fun foo() = 42

    @JsName("bar__") override val bar = 142

    override val baz: Int
        @JsName("getBaz__") get() = 155
}
