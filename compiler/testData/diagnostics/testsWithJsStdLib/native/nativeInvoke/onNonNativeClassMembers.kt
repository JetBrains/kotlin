// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun foo()<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun invoke(a: String): Int<!> = 0

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun Int.ext()<!> = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun Int.invoke(a: String, b: Int)<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    val foo<!> = 0

    nativeInvoke
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}

    default object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun foo()<!> {}

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun invoke(a: String): Int<!> = 0

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun Int.ext()<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun Int.invoke(a: String, b: Int)<!> = "OK"
    }
}