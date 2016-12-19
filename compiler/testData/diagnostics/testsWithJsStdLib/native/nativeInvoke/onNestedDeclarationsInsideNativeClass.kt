// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class C {
            @nativeInvoke
            fun foo() { noImpl }

            @nativeInvoke
            fun invoke(a: String): Int = noImpl
        }

        object obj {
            @nativeInvoke
            fun foo() { noImpl }

            @nativeInvoke
            fun invoke(a: String): Int = noImpl
        }

        companion object {
            @nativeInvoke
            fun foo() { noImpl }

            @nativeInvoke
            fun invoke(a: String): Int = noImpl
        }
    }
}