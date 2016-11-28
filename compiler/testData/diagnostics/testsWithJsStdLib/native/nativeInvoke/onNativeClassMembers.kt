// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    @nativeInvoke
    fun foo() {}

    @nativeInvoke
    fun invoke(a: String): Int = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val foo = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj1 {}

    companion object {
        @nativeInvoke
        fun foo() {}

        @nativeInvoke
        fun invoke(a: String): Int = 0

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        val foo = 0

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        object Obj2 {}
    }
}