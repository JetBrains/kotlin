// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

class A {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
    fun foo()<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
    fun invoke(a: String): Int<!> = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    val baz = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeInvoke<!>
    object Obj {}

    companion object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
        fun foo()<!> {}

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
        fun invoke(a: String): Int<!> = 0
    }
}
