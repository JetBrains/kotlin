// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    class A {
        @nativeInvoke
        fun foo() {}

        @nativeInvoke
        fun invoke(a: String): Int = 0

        @nativeInvoke
        fun Int.ext() = 1

        @nativeInvoke
        fun Int.invoke(a: String, b: Int) = "OK"

        val anonymous = object {
            @nativeInvoke
            fun foo() {}

            @nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}
