// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    @nativeInvoke
    fun foo() {noImpl}

    @nativeInvoke
    fun invoke(a: String): Int = noImpl

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val foo: Int = noImpl

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj1 {}

    companion object {
        @nativeInvoke
        fun foo() { noImpl }

        @nativeInvoke
        fun invoke(a: String): Int = noImpl

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        val foo: Int = noImpl

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        object Obj2 {}
    }
}