// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

class A {
    @nativeInvoke
    fun foo() {}

    @nativeInvoke
    fun invoke(a: String): Int = 0

    @nativeInvoke
    fun Int.ext() = 1

    @nativeInvoke
    fun Int.invoke(a: String, b: Int) = "OK"

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val baz = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj {}

    companion object {
        @nativeInvoke
        fun foo() {}

        @nativeInvoke
        fun invoke(a: String): Int = 0

        @nativeInvoke
        fun Int.ext() = 1

        @nativeInvoke
        fun Int.invoke(a: String, b: Int) = "OK"
    }
}
