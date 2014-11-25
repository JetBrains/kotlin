// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    class B {
        class C {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        object obj {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        class object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        val anonymous = object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }
    }
}