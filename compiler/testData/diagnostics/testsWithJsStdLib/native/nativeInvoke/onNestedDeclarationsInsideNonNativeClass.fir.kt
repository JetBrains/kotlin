// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

class A {
    class B {
        class C {
            @nativeInvoke
            fun foo() {}

            @nativeInvoke
            fun invoke(a: String): Int = 0

            @nativeInvoke
            fun Int.ext() = 1

            @nativeInvoke
            fun Int.invoke(a: String, b: Int) = "OK"
        }

        object obj {
            @nativeInvoke
            fun foo() {}

            @nativeInvoke
            fun invoke(a: String): Int = 0
        }

        companion object {
            @nativeInvoke
            fun foo() {}

            @nativeInvoke
            fun invoke(a: String): Int = 0
        }

        val anonymous = object {
            @nativeInvoke
            fun foo() {}

            @nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}
