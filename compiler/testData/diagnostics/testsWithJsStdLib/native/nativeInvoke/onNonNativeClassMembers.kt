// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

class A {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun foo()<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun invoke(a: String): Int<!> = 0

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun Int.ext()<!> = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
    fun Int.invoke(a: String, b: Int)<!> = "OK"

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val baz = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj {}

    companion object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun foo()<!> {}

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun invoke(a: String): Int<!> = 0

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.ext()<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.invoke(a: String, b: Int)<!> = "OK"
    }
}