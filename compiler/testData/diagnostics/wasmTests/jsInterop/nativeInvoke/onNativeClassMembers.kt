// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

external class A {
    @nativeInvoke
    fun foo()

    @nativeInvoke
    fun invoke(a: String): Int

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val foo: Int

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj1 {}

    companion object {
        @nativeInvoke
        fun foo()

        @nativeInvoke
        fun invoke(a: String): Int

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        val foo: Int

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        object Obj2 {}
    }
}