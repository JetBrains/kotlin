// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    @nativeInvoke
    fun foo() {}

    @nativeInvoke
    fun invoke(a: String): Int = 0

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun Int.ext()<!> = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun Int.invoke(a: String, b: Int)<!> = "OK"

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val foo = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj1 {}

    companion object {
        @nativeInvoke
        fun foo() {}

        @nativeInvoke
        fun invoke(a: String): Int = 0

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.ext()<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.invoke(a: String, b: Int)<!> = "OK"

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        val foo = 0

        <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
        object Obj2 {}
    }
}