// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    class B {
        class C {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        object obj {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        class object {
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
