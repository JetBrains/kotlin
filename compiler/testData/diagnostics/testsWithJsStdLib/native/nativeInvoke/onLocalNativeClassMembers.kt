// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    [native]
    class A {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0

        val anonymous = object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}