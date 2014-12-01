// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    [native]
    class A {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun Int.ext()<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
        fun Int.invoke(a: String, b: Int)<!> = "OK"

        val anonymous = object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}