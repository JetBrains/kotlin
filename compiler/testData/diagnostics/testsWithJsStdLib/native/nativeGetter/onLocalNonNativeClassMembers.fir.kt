// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    class A {
        @nativeGetter
        fun get(a: String): Any? = null

        @nativeGetter
        fun take(a: Number): String? = null

        @nativeGetter
        fun foo(a: Double): String? = null
    }

    class B {
        <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
        val foo = 0
    }

    class C {
        @nativeGetter
        fun Int.get(a: String): Int? = 1

        @nativeGetter
        fun Int.get2(a: Number): String? = "OK"

        @nativeGetter
        fun Int.get3(a: Int): String? = "OK"

        @nativeGetter
        fun get(): Any? = null

        @nativeGetter
        fun get(a: A): Any? = null

        @nativeGetter
        fun foo(a: Int) {}

        @nativeGetter
        fun bar(a: String): Int = 0

        @nativeGetter
        fun baz(a: Int = 0): Int? = 0
    }
}
