// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    @nativeInvoke
    fun foo() {definedExternally}

    @nativeInvoke
    fun invoke(a: String): Int = definedExternally

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val foo: Int = definedExternally

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj1 {}

    companion object {
        @nativeInvoke
        fun foo() { definedExternally }

        @nativeInvoke
        fun invoke(a: String): Int = definedExternally

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        val foo: Int = definedExternally

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        object Obj2 {}
    }
}