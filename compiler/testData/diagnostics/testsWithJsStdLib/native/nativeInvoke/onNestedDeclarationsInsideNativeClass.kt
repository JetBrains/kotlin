// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    class B {
        class C {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun Int.ext()<!> = 1

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun Int.invoke(a: String, b: Int)<!> = "OK"
        }

        object obj {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun Int.ext()<!> = 1

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun Int.invoke(a: String, b: Int)<!> = "OK"
        }

        default object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        val anonymous = object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}