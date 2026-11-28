// RUN_PIPELINE_TILL: CODEGEN
package foo

// @JsName on both the open declaration and the override
open external class A {
    @JsName("foo_") open fun foo(): Int

    @JsName("bar_") open val bar: Int

    open val baz: Int
        @JsName("getBaz_") get
}

external class B : A {
    @JsName("foo__") override fun foo(): Int

    @JsName("bar__") override val bar: Int

    override val baz: Int
        @JsName("getBaz__") get
}

// @JsName only on the open declaration
open external class C {
    @JsName("foo_") open fun foo(): Int

    @JsName("bar_") open val bar: Int

    open val baz: Int
        @JsName("getBaz_") get
}

external class D : C {
    override fun foo(): Int

    override val bar: Int

    override val baz: Int
}

// @JsName only on the override
open external class E {
    open fun foo(): Int

    open val bar: Int

    open val baz: Int
}

external class F : E {
    @JsName("foo_") override fun foo(): Int

    @JsName("bar_") override val bar: Int

    override val baz: Int
        @JsName("getBaz_") get
}
