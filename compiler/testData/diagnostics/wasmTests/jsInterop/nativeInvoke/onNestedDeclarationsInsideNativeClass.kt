// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION
@file:Suppress("OPT_IN_USAGE")

external class A {
    class B {
        class C {
            @nativeInvoke
            fun foo()

            @nativeInvoke
            fun invoke(a: String): Int
        }

        object obj {
            @nativeInvoke
            fun foo()

            @nativeInvoke
            fun invoke(a: String): Int
        }

        companion object {
            @nativeInvoke
            fun foo()

            @nativeInvoke
            fun invoke(a: String): Int
        }
    }
}