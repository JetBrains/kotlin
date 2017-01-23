// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class C {
            @nativeInvoke
            fun foo() { definedExternally }

            @nativeInvoke
            fun invoke(a: String): Int = definedExternally
        }

        object obj {
            @nativeInvoke
            fun foo() { definedExternally }

            @nativeInvoke
            fun invoke(a: String): Int = definedExternally
        }

        companion object {
            @nativeInvoke
            fun foo() { definedExternally }

            @nativeInvoke
            fun invoke(a: String): Int = definedExternally
        }
    }
}