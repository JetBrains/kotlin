// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    class A {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun foo()<!> {}

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun invoke(a: String): Int<!> = 0

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.ext()<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
        fun Int.invoke(a: String, b: Int)<!> = "OK"

        val anonymous = object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }
    }
}
