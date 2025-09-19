// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

class A {
    class B {
        class C {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        object obj {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        companion object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }

        val anonymous = object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun foo()<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke
            fun invoke(a: String): Int<!> = 0
        }
    }
}